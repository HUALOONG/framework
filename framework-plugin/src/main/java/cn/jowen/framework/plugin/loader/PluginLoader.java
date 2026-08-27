package cn.jowen.framework.plugin.loader;

import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.descriptor.PluginDescriptorLoader;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * 插件加载器。解析描述符 → 校验 → 创建 ClassLoader → 实例化主类。
 *
 * <p>实现 {@link AutoCloseable} 以便在热部署时关闭旧加载器。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginLoader implements AutoCloseable {

    private final PluginDescriptor descriptor;
    private final PluginClassLoader classLoader;
    private final Path pluginPath;

    /**
     * 构造插件加载器。
     *
     * @param descriptor 插件描述符，不可为 {@code null}
     * @param urls       插件自身资源 URL，可为空
     * @param parent     父加载器（通常为应用加载器）
     * @param strategy   加载策略，不可为 {@code null}
     */
    public PluginLoader(PluginDescriptor descriptor, URL[] urls, ClassLoader parent, ClassLoadingStrategy strategy) {
        this.descriptor = descriptor;
        this.pluginPath = null;
        this.classLoader = createClassLoader(descriptor, urls, parent, strategy);
    }

    /**
     * 默认策略构造器（FRAMEWORK_API_DELEGATE）。
     *
     * @param descriptor 插件描述符
     * @param urls       插件 URL
     * @param parent     父加载器
     */
    public PluginLoader(PluginDescriptor descriptor, URL[] urls, ClassLoader parent) {
        this(descriptor, urls, parent, ClassLoadingStrategy.FRAMEWORK_API_DELEGATE);
    }

    /**
     * 从 JAR 文件构造插件加载器。
     *
     * @param jarPath  JAR 路径，不可为 {@code null}
     * @param parent   父加载器，不可为 {@code null}
     * @param strategy 加载策略，不可为 {@code null}
     * @throws IOException JAR 读取失败时抛出
     */
    public PluginLoader(Path jarPath, ClassLoader parent, ClassLoadingStrategy strategy) throws IOException {
        this.pluginPath = jarPath;
        PluginDescriptor desc = loadDescriptor(jarPath);
        this.descriptor = desc;
        URL[] urls = {jarPath.toUri().toURL()};
        this.classLoader = createClassLoader(desc, urls, parent, strategy);
    }

    private static PluginDescriptor loadDescriptor(Path jarPath) throws IOException {
        // 描述符格式探测：优先 plugin.json，否则回退 yaml（与既有行为一致），解析器经统一 SPI 机制发现
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            String suffix = jarFile.getJarEntry("META-INF/plugin/plugin.json") != null ? "json" : "yaml";
            return findParser(suffix).load(jarPath);
        }
    }

    /**
     * 经 {@link ExtensionLoader} 发现描述符解析器，按文件后缀选择。
     *
     * @param suffix 描述符文件后缀（json 或 yaml）
     * @return 匹配的解析器，不可为 {@code null}
     */
    private static PluginDescriptorLoader findParser(String suffix) {
        ExtensionLoader<PluginDescriptorLoader> loader =
                ExtensionLoader.getExtensionLoader(PluginDescriptorLoader.class);
        for (PluginDescriptorLoader parser : loader.getAllExtensions()) {
            if (parser.supportedExtensions().contains(suffix)) {
                return parser;
            }
        }
        throw new IllegalStateException("未找到支持 " + suffix + " 描述符的解析器");
    }

    private static List<String> computeExportedPackages(PluginDescriptor descriptor) {
        List<String> merged = new ArrayList<>();
        merged.add("cn.jowen.framework");
        return List.copyOf(merged);
    }

    private static PluginClassLoader createClassLoader(PluginDescriptor descriptor, URL[] urls,
                                                       ClassLoader parent, ClassLoadingStrategy strategy) {
        List<String> exported = computeExportedPackages(descriptor);
        return switch (strategy) {
            case CHILD_FIRST -> new FrameworkApiDelegateClassLoader(urls, parent, List.of());
            case FRAMEWORK_API_DELEGATE, PARENT_FIRST -> new FrameworkApiDelegateClassLoader(urls, parent, exported);
        };
    }

    /**
     * @return 关联描述符，不可为 {@code null}
     */
    public PluginDescriptor descriptor() {
        return descriptor;
    }

    /**
     * @return 插件 jar 路径，可能为 {@code null}
     */
    public Path getPluginPath() {
        return pluginPath;
    }

    /**
     * @return 内部类加载器
     */
    public PluginClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * 加载并实例化插件主类。
     *
     * @return 插件实例
     * @throws PluginLoadException 加载/实例化失败时抛出
     */
    public Plugin load() {
        try {
            Class<?> clazz = classLoader.loadClass(descriptor.pluginClass());
            if (!Plugin.class.isAssignableFrom(clazz)) {
                throw new PluginLoadException("插件类未实现 Plugin 接口：" + descriptor.pluginClass());
            }
            return (Plugin) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new PluginLoadException("加载插件失败：" + descriptor.pluginId(), e);
        }
    }

    /**
     * @return 资源 URL（转发到内部类加载器）
     */
    public URL getResource(String name) {
        return classLoader.getResource(name);
    }

    /**
     * @return 资源 URL 枚举（转发到内部类加载器）
     */
    public java.util.Enumeration<URL> getResources(String name) throws IOException {
        return classLoader.getResources(name);
    }

    /**
     * 关闭内部类加载器并释放资源。
     */
    @Override
    public void close() throws IOException {
        classLoader.close();
    }

    /**
     * 插件加载异常。
     */
    public static final class PluginLoadException extends RuntimeException {
        public PluginLoadException(String message) {
            super(message);
        }

        public PluginLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
