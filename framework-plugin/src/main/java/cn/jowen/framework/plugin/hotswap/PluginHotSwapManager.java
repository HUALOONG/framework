package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.api.PluginManager;
import org.jspecify.annotations.NullMarked;

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
    private final Map<String, Long> lastChangeTime = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private Thread watchThread;
    private WatchServiceWrapper watchService;

    /**
     * 构造热部署管理器。
     *
     * @param pluginsDir    插件目录
     * @param pluginManager 插件管理器
     * @param strategy      热部署策略
     * @param debounceMs    防抖间隔（毫秒）
     */
    public PluginHotSwapManager(Path pluginsDir, PluginManager pluginManager,
                                HotSwapStrategy strategy, long debounceMs) {
        this.pluginsDir = pluginsDir;
        this.pluginManager = pluginManager;
        this.strategy = strategy;
        this.debounceMs = debounceMs;
    }

    /**
     * 启动文件监听。
     */
    public void start() {
        if (running) return;
        running = true;
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
        Path jarPath = pluginsDir.resolve(fileName);
        if (!Files.isRegularFile(jarPath)) return;
        try {
            if (strategy == HotSwapStrategy.RESTART) {
                // 提取插件 id（从文件名或描述符）
                String pluginId = extractPluginId(fileName);
                if (pluginId != null) {
                    pluginManager.restartPlugin(pluginId);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("热部署失败：" + fileName + "，原因：" + e.getMessage());
        }
    }

    private String extractPluginId(String fileName) {
        // 简化实现：从文件名去掉 .jar 后缀
        return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    @Override
    public void close() {
        running = false;
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
