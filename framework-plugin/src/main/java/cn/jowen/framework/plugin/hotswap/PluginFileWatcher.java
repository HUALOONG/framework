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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * 插件文件监听器。基于 JDK WatchService 监听目录变化，支持创建/修改/删除事件回调 + 防抖。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginFileWatcher implements Closeable {

    private final Path watchDir;
    private final FileChangeHandler handler;
    private final DebounceTimer debounceTimer;
    private final WatchService watchService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "plugin-file-watcher");
        t.setDaemon(true);
        return t;
    });
    private volatile Thread watcherThread;

    public PluginFileWatcher(Path watchDir, FileChangeHandler handler, long debounceMs) {
        this.watchDir = watchDir;
        this.handler = handler;
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
                    ENTRY_CREATE,
                    ENTRY_DELETE,
                    ENTRY_MODIFY);
        } catch (Exception e) {
            throw new RuntimeException("注册 WatchKey 失败：" + watchDir, e);
        }
        watcherThread = new Thread(this::watchLoop, "plugin-file-watcher-loop");
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
                String fileName = changed.getFileName().toString();
                debounceTimer.runOnce(() -> {
                    if (running.get()) {
                        if (event.kind() == ENTRY_CREATE) handler.onFileCreated(fileName);
                        else if (event.kind() == ENTRY_MODIFY) handler.onFileModified(fileName);
                        else if (event.kind() == ENTRY_DELETE) handler.onFileDeleted(fileName);
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
        try {
            watchService.close();
        } catch (Exception ignored) {
        }
        if (watcherThread != null && watcherThread.isAlive()) {
            try {
                watcherThread.join(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 文件变化处理器。
     */
    public interface FileChangeHandler {
        void onFileCreated(String fileName);

        void onFileModified(String fileName);

        void onFileDeleted(String fileName);
    }
}
