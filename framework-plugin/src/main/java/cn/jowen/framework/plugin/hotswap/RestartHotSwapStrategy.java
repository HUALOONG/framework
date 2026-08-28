package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.context.DefaultPluginContext;
import cn.jowen.framework.plugin.context.MapPluginConfiguration;
import cn.jowen.framework.plugin.context.SharedData;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.loader.ClassLoadingStrategy;
import cn.jowen.framework.plugin.loader.PluginLoader;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重启策略：文件变更时先卸载旧版本，再经 {@link PluginLoader} 加载新 jar，初始化并启动插件。
 *
 * <p>类加载器由本策略持有（随插件生命周期存活），卸载时释放旧类加载器，关闭时统一清理。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class RestartHotSwapStrategy implements PluginFileWatcher.FileChangeHandler, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartHotSwapStrategy.class);

    private final PluginManager manager;
    private final Path pluginsDir;
    private final @Nullable PluginSpiBridge spiBridge;
    private final ClassLoader applicationClassLoader;
    private final Map<String, PluginLoader> loaders = new ConcurrentHashMap<>();
    private final Map<String, String> jarFileByPluginId = new ConcurrentHashMap<>();

    /**
     * 构造重启策略（不桥接 core SPI，等价于桥接参数为 {@code null}）。
     *
     * @param manager    插件管理器，不可为 {@code null}
     * @param pluginsDir 插件目录，不可为 {@code null}
     */
    public RestartHotSwapStrategy(PluginManager manager, Path pluginsDir) {
        this(manager, pluginsDir, null);
    }

    /**
     * 构造重启策略。
     *
     * @param manager    插件管理器，不可为 {@code null}
     * @param pluginsDir 插件目录，不可为 {@code null}
     * @param spiBridge  扩展点桥接门面（可为 {@code null}，置空时热加载/卸载不注册/注销描述符扩展点映射）
     */
    public RestartHotSwapStrategy(PluginManager manager, Path pluginsDir, @Nullable PluginSpiBridge spiBridge) {
        this.manager = manager;
        this.pluginsDir = pluginsDir;
        this.spiBridge = spiBridge;
        this.applicationClassLoader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * 兼容入口：视为文件修改。
     *
     * @param fileName 文件名，不可为 {@code null}
     */
    public void onPluginChange(String fileName) {
        onFileModified(fileName);
    }

    @Override
    public void onFileCreated(String fileName) {
        loadPlugin(fileName);
    }

    @Override
    public void onFileModified(String fileName) {
        loadPlugin(fileName);
    }

    @Override
    public void onFileDeleted(String fileName) {
        unloadPluginByJar(fileName);
    }

    private void loadPlugin(String fileName) {
        Path jarPath = pluginsDir.resolve(fileName);
        if (!Files.isRegularFile(jarPath)) {
            return;
        }
        try {
            PluginLoader loader = new PluginLoader(jarPath, applicationClassLoader,
                    ClassLoadingStrategy.FRAMEWORK_API_DELEGATE);
            PluginDescriptor desc = loader.descriptor();
            String id = desc.pluginId();
            if (manager.getPlugin(id) != null) {
                // 同 id 已加载：先注销旧版本桥接映射并卸载，再加载新版本
                unregisterPluginPoints(id);
                manager.unloadPlugin(id);
            }
            PluginLoader oldLoader = loaders.remove(id);
            if (oldLoader != null) {
                oldLoader.close();
            }
            Plugin plugin = loader.load();
            PluginContext context = new DefaultPluginContext(id, desc, new MapPluginConfiguration(),
                    new SharedData(), manager, applicationClassLoader, loader.getClassLoader(), null);
            manager.initialize(id, plugin, context);
            loaders.put(id, loader);
            jarFileByPluginId.put(id, fileName);
            manager.startPlugin(id);
            registerPluginPoints(id, desc);
            LOGGER.info("插件热部署加载完成：" + id);
        } catch (Exception e) {
            LOGGER.warn("插件热部署加载失败：" + fileName + "，原因：" + e.getMessage());
        }
    }

    private void unloadPluginByJar(String fileName) {
        for (Map.Entry<String, String> entry : jarFileByPluginId.entrySet()) {
            if (entry.getValue().equals(fileName)) {
                String id = entry.getKey();
                // 先注销桥接映射与插件实例，再释放类加载器
                unregisterPluginPoints(id);
                manager.unloadPlugin(id);
                PluginLoader loader = loaders.remove(id);
                if (loader != null) {
                    try {
                        loader.close();
                    } catch (IOException e) {
                        LOGGER.warn("插件类加载器关闭失败：" + id + "，原因：" + e.getMessage());
                    }
                }
                jarFileByPluginId.remove(id);
                LOGGER.info("插件热部署卸载完成：" + id);
                return;
            }
        }
    }

    /**
     * 将插件的描述符扩展点映射注册到桥接门面（桥接侧按插件引用计数，全插件注销后源才移除）。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     * @param desc     插件描述符，不可为 {@code null}
     */
    private void registerPluginPoints(String pluginId, PluginDescriptor desc) {
        if (spiBridge == null) {
            return;
        }
        spiBridge.registerPlugin(pluginId, desc.extensionPoints());
    }

    /**
     * 注销该插件经桥接注册的全部扩展点映射。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     */
    private void unregisterPluginPoints(String pluginId) {
        if (spiBridge == null) {
            return;
        }
        spiBridge.unregisterPlugin(pluginId);
    }

    @Override
    public void close() throws IOException {
        // 关闭即视为全部插件卸载，逐插件注销桥接映射并释放类加载器
        for (String id : List.copyOf(loaders.keySet())) {
            unregisterPluginPoints(id);
        }
        for (PluginLoader loader : loaders.values()) {
            loader.close();
        }
        loaders.clear();
        jarFileByPluginId.clear();
    }
}