package cn.jowen.framework.boot.autoconfigure.plugin;

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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 插件自动加载引导器：容器就绪后扫描 {@code framework.plugin.plugins-dir}，按依赖顺序加载 jar、
 * 初始化并按 {@code autoStart} 启动插件；容器关闭时统一释放各插件类加载器。
 *
 * <p>单插件加载失败不影响其余插件与应用启动；插件目录不存在时静默跳过。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class PluginBootstrap implements SmartInitializingSingleton, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginBootstrap.class);

    private final PluginManager pluginManager;
    private final BootPluginProperties properties;
    private final @Nullable PluginSpiBridge spiBridge;
    private final ClassLoader applicationClassLoader;
    private final Map<String, PluginLoader> loaders = new ConcurrentHashMap<>();

    /**
     * 构造引导器（无桥接门面，等价于 {@code new PluginBootstrap(pluginManager, properties, null)}）。
     *
     * @param pluginManager 插件管理器，不可为 {@code null}
     * @param properties    插件配置，不可为 {@code null}
     */
    public PluginBootstrap(PluginManager pluginManager, BootPluginProperties properties) {
        this(pluginManager, properties, null);
    }

    /**
     * 构造引导器。
     *
     * @param pluginManager 插件管理器，不可为 {@code null}
     * @param properties    插件配置，不可为 {@code null}
     * @param spiBridge     扩展点桥接门面（可为 {@code null}，置空时不注册描述符扩展点映射）
     */
    public PluginBootstrap(PluginManager pluginManager, BootPluginProperties properties,
                           @Nullable PluginSpiBridge spiBridge) {
        Objects.requireNonNull(pluginManager, "pluginManager must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        this.pluginManager = pluginManager;
        this.properties = properties;
        this.spiBridge = spiBridge;
        this.applicationClassLoader = Objects.requireNonNullElse(
                Thread.currentThread().getContextClassLoader(), PluginBootstrap.class.getClassLoader());
    }

    @Override
    public void afterSingletonsInstantiated() {
        Path pluginsDir = Path.of(properties.getPluginsDir());
        if (!Files.isDirectory(pluginsDir)) {
            LOGGER.info("插件目录不存在，跳过自动加载：" + properties.getPluginsDir());
            return;
        }
        ClassLoadingStrategy strategy = parseStrategy(properties.getClassLoading().getStrategy());
        List<Path> jars = listJars(pluginsDir);
        if (jars.isEmpty()) {
            LOGGER.info("插件目录为空，无待加载插件：" + properties.getPluginsDir());
            return;
        }
        for (Path jar : jars) {
            loadOne(jar, strategy);
        }
    }

    private void loadOne(Path jar, ClassLoadingStrategy strategy) {
        try {
            PluginLoader loader = new PluginLoader(jar, applicationClassLoader, strategy);
            PluginDescriptor desc = loader.descriptor();
            String id = desc.pluginId();
            if (properties.getDisabledPlugins().contains(id)) {
                loader.close();
                LOGGER.info("插件已禁用，跳过：" + id);
                return;
            }
            if (pluginManager.getPlugin(id) != null) {
                loader.close();
                LOGGER.warn("插件已加载，跳过重复：" + id);
                return;
            }
            Plugin plugin = loader.load();
            PluginContext context = new DefaultPluginContext(id, desc, new MapPluginConfiguration(),
                    new SharedData(), pluginManager, applicationClassLoader,
                    loader.getClassLoader(), null);
            pluginManager.initialize(id, plugin, context);
            loaders.put(id, loader);
            // 描述符声明的扩展点映射注册到桥接门面，使该插件的扩展对 core SPI 查询可见
            registerExtensionPoints(id, desc);
            if (properties.isAutoStart()) {
                pluginManager.startPlugin(id);
            }
            LOGGER.info("插件加载完成：" + id);
        } catch (Exception e) {
            // 单插件失败不阻断其余加载
            LOGGER.warn("插件加载失败：" + jar + "，原因：" + e.getMessage());
        }
    }

    @Override
    public void destroy() {
        // 先解除桥接源，再关闭各插件类加载器
        if (spiBridge != null) {
            spiBridge.clear();
        }
        for (Map.Entry<String, PluginLoader> entry : loaders.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                LOGGER.warn("插件类加载器关闭失败：" + entry.getKey() + "，原因：" + e.getMessage());
            }
        }
        loaders.clear();
    }

    /**
     * 将插件描述符中的扩展点映射按插件注册到桥接门面（引用计数，随插件销毁批量注销）。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     * @param desc     插件描述符，不可为 {@code null}
     */
    private void registerExtensionPoints(String pluginId, PluginDescriptor desc) {
        if (spiBridge == null) {
            return;
        }
        spiBridge.registerPlugin(pluginId, desc.extensionPoints());
    }

    private static List<Path> listJars(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
        } catch (IOException e) {
            LOGGER.warn("扫描插件目录失败：" + dir + "，原因：" + e.getMessage());
            return List.of();
        }
    }

    private static ClassLoadingStrategy parseStrategy(String strategy) {
        try {
            return ClassLoadingStrategy.valueOf(strategy.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return ClassLoadingStrategy.FRAMEWORK_API_DELEGATE;
        }
    }
}