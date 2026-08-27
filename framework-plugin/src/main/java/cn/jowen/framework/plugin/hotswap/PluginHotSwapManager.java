package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.hotswap.PluginFileWatcher.FileChangeHandler;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件热部署管理器。基于 JDK WatchService 监听目录变化。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginHotSwapManager implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginHotSwapManager.class);

    private final Path pluginsDir;
    private final PluginManager pluginManager;
    private final HotSwapStrategy strategy;
    private final long debounceMs;
    private final @Nullable PluginSpiBridge spiBridge;
    private final Map<String, Long> lastChangeTime = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private @Nullable FileChangeHandler handler;
    private Thread watchThread;
    private WatchServiceWrapper watchService;

    /**
     * 构造热部署管理器（不桥接 core SPI，等价于桥接参数为 {@code null}）。
     *
     * @param pluginsDir    插件目录
     * @param pluginManager 插件管理器
     * @param strategy      热部署策略
     * @param debounceMs    防抖间隔（毫秒）
     */
    public PluginHotSwapManager(Path pluginsDir, PluginManager pluginManager,
                                HotSwapStrategy strategy, long debounceMs) {
        this(pluginsDir, pluginManager, strategy, debounceMs, null);
    }

    /**
     * 构造热部署管理器。
     *
     * @param pluginsDir    插件目录
     * @param pluginManager 插件管理器
     * @param strategy      热部署策略
     * @param debounceMs    防抖间隔（毫秒）
     * @param spiBridge     扩展点桥接门面（可为 {@code null}；置空时热加载/卸载不注册/注销描述符扩展点映射）
     */
    public PluginHotSwapManager(Path pluginsDir, PluginManager pluginManager,
                                HotSwapStrategy strategy, long debounceMs,
                                @Nullable PluginSpiBridge spiBridge) {
        this.pluginsDir = pluginsDir;
        this.pluginManager = pluginManager;
        this.strategy = strategy;
        this.debounceMs = debounceMs;
        this.spiBridge = spiBridge;
    }

    /**
     * 启动文件监听。
     */
    public void start() {
        if (running) return;
        running = true;
        this.handler = switch (strategy) {
            case RESTART -> new RestartHotSwapStrategy(pluginManager, pluginsDir, spiBridge);
            case MANUAL -> new ManualHotSwapStrategy();
            case RELOAD_CLASSES -> null; // 需 JVM HotSwap 支持，暂不自动处理
        };
        watchThread = new Thread(this::watchLoop, "plugin-hotswap-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    private void watchLoop() {
        try {
            WatchServiceWrapper ws = new WatchServiceWrapper(pluginsDir);
            this.watchService = ws;
            while (running) {
                WatchServiceWrapper.WatchEvent event = ws.poll(debounceMs);
                if (event == null) continue;
                Path file = event.path();
                if (file == null || !file.toString().endsWith(".jar")) continue;
                String fileName = file.getFileName().toString();
                // 防抖
                Long last = lastChangeTime.get(fileName);
                if (last != null && System.currentTimeMillis() - last < debounceMs) continue;
                lastChangeTime.put(fileName, System.currentTimeMillis());
                // 执行热部署
                onFileChanged(fileName);
            }
        } catch (Exception e) {
            LOGGER.warn("热部署监听异常：" + e.getMessage());
        }
    }

    private void onFileChanged(String fileName) {
        FileChangeHandler h = handler;
        if (h == null) {
            return;
        }
        Path jarPath = pluginsDir.resolve(fileName);
        // 文件仍存在视为新增/修改（重载），已删除则卸载
        if (Files.isRegularFile(jarPath)) {
            h.onFileModified(fileName);
        } else {
            h.onFileDeleted(fileName);
        }
    }

    @Override
    public void close() {
        running = false;
        // 关闭即视为全部插件卸载，清理桥接映射（幂等）
        if (spiBridge != null) {
            spiBridge.clear();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }
        if (watchThread != null && watchThread.isAlive()) {
            try {
                watchThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (handler instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOGGER.warn("热部署策略关闭失败：" + e.getMessage());
            }
        }
        handler = null;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * 内部 WatchService 包装。
     */
    private static class WatchServiceWrapper implements Closeable {
        private final java.nio.file.WatchService watchService;
        private final Path dir;
        private final Thread thread;
        private volatile boolean open = true;
        private volatile Path lastEvent;

        WatchServiceWrapper(Path dir) throws IOException {
            this.dir = dir;
            this.watchService = dir.getFileSystem().newWatchService();
            dir.register(watchService,
                    java.nio.file.StandardWatchEventKinds.ENTRY_CREATE,
                    java.nio.file.StandardWatchEventKinds.ENTRY_DELETE,
                    java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);
            thread = new Thread(this::loop, "watch-service-loop");
            thread.setDaemon(true);
            thread.start();
        }

        private void loop() {
            while (open) {
                try {
                    java.nio.file.WatchKey key = watchService.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (key == null) continue;
                    for (java.nio.file.WatchEvent<?> event : key.pollEvents()) {
                        Object context = event.context();
                        if (context instanceof java.nio.file.Path p) {
                            lastEvent = p;
                        }
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        WatchEvent poll(long timeoutMs) {
            Path event = lastEvent;
            lastEvent = null;
            return event != null ? new WatchEvent(event) : null;
        }

        @Override
        public void close() throws IOException {
            open = false;
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }

        record WatchEvent(Path path) {
        }
    }
}
