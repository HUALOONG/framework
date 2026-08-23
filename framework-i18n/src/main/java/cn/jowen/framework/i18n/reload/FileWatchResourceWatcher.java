package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Locale;

/**
 * 文件系统热加载监听器。基于 {@link WatchService} 监听目录下 {@code *.properties} 文件变更
 * （新增/修改/删除），经去抖窗口合并后触发 {@link ReloadableMessageSource#reload()}。
 *
 * <p>变更文件以 {@code basename[_语言[_国家]].properties} 命名，重载由底层消息源按区域
 * 重新加载；不匹配 {@code *.properties} 的文件变更被忽略。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class FileWatchResourceWatcher implements ResourceWatcher {

    private static final String PROPERTIES_SUFFIX = ".properties";

    private final Path directory;
    private final ReloadableMessageSource target;
    private final ResourceReloader reloader;
    private final Duration debounce;

    private volatile boolean running;
    private volatile @Nullable WatchService watchService;
    private volatile @Nullable Thread worker;

    /**
     * 构造监听器。
     *
     * @param directory 监听目录，不可为 {@code null} 且须存在
     * @param target    重载目标消息源，不可为 {@code null}
     * @param reloader  重载执行器（负责事件发布），不可为 {@code null}
     * @param debounce  去抖窗口：窗口内多次变更合并为一次重载，不可为 {@code null}
     */
    public FileWatchResourceWatcher(Path directory, ReloadableMessageSource target,
                                    ResourceReloader reloader, Duration debounce) {
        if (!java.nio.file.Files.isDirectory(directory)) {
            throw new IllegalArgumentException("监听目录不存在或不是目录: " + directory);
        }
        this.directory = directory;
        this.target = target;
        this.reloader = reloader;
        this.debounce = debounce;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        try {
            WatchService service = FileSystems.getDefault().newWatchService();
            directory.register(service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            this.watchService = service;
            this.running = true;
            Thread thread = Thread.ofPlatform()
                    .name("i18n-file-watch-" + directory.getFileName())
                    .daemon(true)
                    .unstarted(this::poll);
            this.worker = thread;
            thread.start();
        } catch (IOException ex) {
            throw new UncheckedIOException("注册文件监听失败: " + directory, ex);
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        WatchService service = watchService;
        Thread thread = worker;
        if (service != null) {
            try {
                service.close();
            } catch (IOException ignored) {
                // 关闭过程中的 IO 异常可忽略
            }
        }
        if (thread != null) {
            thread.interrupt();
        }
        watchService = null;
        worker = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 监听目录。
     */
    public Path getDirectory() {
        return directory;
    }

    private void poll() {
        WatchService service = watchService;
        if (service == null) {
            return;
        }
        while (running) {
            try {
                WatchKey key = service.take();
                boolean changed = drainEvents(key);
                if (!key.reset()) {
                    continue;
                }
                if (changed) {
                    reloadAfterDebounce();
                }
            } catch (InterruptedException | ClosedWatchServiceException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean drainEvents(WatchKey key) {
        boolean changed = false;
        for (WatchEvent<?> event : key.pollEvents()) {
            Path filename = (Path) event.context();
            if (filename != null && filename.toString().endsWith(PROPERTIES_SUFFIX)) {
                changed = true;
            }
        }
        return changed;
    }

    private void reloadAfterDebounce() {
        try {
            Thread.sleep(debounce.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        reloader.reload(target, (Locale) null);
    }
}
