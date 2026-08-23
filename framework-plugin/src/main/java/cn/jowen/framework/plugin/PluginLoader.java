package cn.jowen.framework.plugin;

import cn.jowen.framework.plugin.classloader.FrameworkApiDelegateClassLoader;
import cn.jowen.framework.plugin.classloader.IsolatedClassLoader;
import cn.jowen.framework.plugin.classloader.PluginClassLoader;
import cn.jowen.framework.plugin.classloader.PluginClassLoaderConfig;
import cn.jowen.framework.plugin.PluginDescriptor;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * 插件类加载器。组合持有三层 {@link PluginClassLoader} 体系（{@link FrameworkApiDelegateClassLoader} /
 * {@link IsolatedClassLoader}），对外隐藏底层类加载实现，仅暴露插件加载与资源查找能力。
 *
 * <p>默认采用 delegate 策略（框架 API 委派父共享），可通过包级重载显式切换 isolated。
 * 不再继承 {@link java.net.URLClassLoader}，避免把内部类加载器当作普通 {@code ClassLoader} 外泄。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginLoader implements AutoCloseable {

    private final PluginDescriptor descriptor;
    private final PluginClassLoader classLoader;

    /** 公共构造器：采用全局默认策略（{@link PluginClassLoaderConfig#strategy()}，默认 delegate）。 */
    public PluginLoader(PluginDescriptor descriptor, URL[] urls, ClassLoader parent) {
        this(descriptor, urls, parent, PluginClassLoaderConfig.strategy());
    }

    /**
     * 包级可见构造器：显式指定加载策略，供 isolated 单测使用。
     *
     * @param descriptor 插件描述符，不可为 {@code null}
     * @param urls       插件自身资源 URL，可为空
     * @param parent     父加载器（通常为框架/应用加载器）
     * @param strategy   加载策略：{@code "delegate"} 或 {@code "isolated"}
     */
    PluginLoader(PluginDescriptor descriptor, URL[] urls, ClassLoader parent, String strategy) {
        this.descriptor = descriptor;
        this.classLoader = createClassLoader(descriptor, urls, parent, strategy);
    }

    /** @return 关联描述符，不可为 {@code null} */
    public PluginDescriptor descriptor() {
        return descriptor;
    }

    /**
     * 加载并实例化插件实现。
     *
     * @return 插件实例，不可为 {@code null}
     * @throws PluginException 加载/实例化失败时抛出
     */
    public Plugin load() {
        try {
            Class<?> clazz = classLoader.loadClass(descriptor.className());
            if (!Plugin.class.isAssignableFrom(clazz)) {
                throw new PluginException("插件类未实现 Plugin 接口：" + descriptor.className());
            }
            return (Plugin) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new PluginException("加载插件失败：" + descriptor.id(), e);
        }
    }

    /** @return 资源 URL（转发到内部类加载器） */
    public URL getResource(String name) {
        return classLoader.getResource(name);
    }

    /** @return 资源 URL 枚举（转发到内部类加载器） */
    public Enumeration<URL> getResources(String name) throws IOException {
        return classLoader.getResources(name);
    }

    /** 关闭内部类加载器并释放资源。 */
    public void close() throws IOException {
        classLoader.close();
    }

    /** 插件异常基类。 */
    public static final class PluginException extends RuntimeException {

        public PluginException(String message) {
            super(message);
        }

        public PluginException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static PluginClassLoader createClassLoader(PluginDescriptor descriptor, URL[] urls,
                                                       ClassLoader parent, String strategy) {
        if ("isolated".equalsIgnoreCase(strategy)) {
            return new IsolatedClassLoader(urls, parent);
        }
        // 默认 delegate：框架 API 委派父共享（导出的包经父加载器解析，避免插件污染框架类）
        return new FrameworkApiDelegateClassLoader(urls, parent, computeExportedPackages(descriptor));
    }

    /**
     * 计算委派给父加载器的导出包集合：必须显式包含 {@code cn.jowen.framework}（框架 API 委派父共享），
     * 再合并全局配置与描述符显式声明的导出包。
     */
    private static List<String> computeExportedPackages(PluginDescriptor descriptor) {
        List<String> merged = new java.util.ArrayList<>();
        merged.add("cn.jowen.framework");
        merged.addAll(PluginClassLoaderConfig.exportedPackages());
        merged.addAll(descriptor.exportedPackages());
        return List.copyOf(merged);
    }
}
