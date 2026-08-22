package cn.jowen.framework.plugin.hotswap;

import org.jspecify.annotations.NullMarked;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 JDK WatchService 的插件目录监听器。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginWatchService implements Closeable {

    private final Path watchDir;
    private final HotSwapStrategy strategy;
    private final DebounceTimer debounceTimer;
    private final WatchService watchService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "plugin-watch-service");
        t.setDaemon(true);
        return t;
    });
    private volatile Thread watcherThread;

    public PluginWatchService(Path watchDir, HotSwapStrategy strategy, long debounceMs) {
        this.watchDir = watchDir;
        this.strategy = strategy;
        this.debounceTimer = new DebounceTimer(debounceMs);
        WatchService ws;
        try {
            ws = watchDir.getFileSystem().newWatchService();
        } catch (Exception e) {
            throw new RuntimeException("创建 WatchService 失败：" + watchDir, e);
        }
        this.watchService = ws;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        try {
            watchDir.register(watchService,
                    WatchEvent.Kind.CREATE,
                    WatchEvent.Kind.DELETE,
                    WatchEvent.Kind.MODIFY);
        } catch (Exception e) {
            throw new RuntimeException("注册 WatchKey 失败：" + watchDir, e);
        }
        watcherThread = new Thread(this::watchLoop, "plugin-watch-loop");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void watchLoop() {
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (key == null) continue;
            for (WatchEvent<?> event : key.pollEvents()) {
                Path changed = (Path) event.context();
                if (changed == null) continue;
                Path full = watchDir.resolve(changed);
                if (!Files.isRegularFile(full)) continue;
                if (!changed.toString().endsWith(".jar")) continue;
                debounceTimer.runOnce(() -> {
                    if (running.get()) {
                        strategy.onPluginChange(changed.getFileName().toString());
                    }
                });
            }
            key.reset();
        }
    }

    @Override
    public void close() {
        running.set(false);
        executor.shutdownNow();
        try { watchService.close(); } catch (Exception ignored) {}
    }
}
