package cn.jowen.framework.plugin;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 插件类加载器。基于 {@link URLClassLoader} 实现插件与框架/应用之间的类加载隔离，
 * 父加载器为框架类加载器（默认线程上下文加载器），避免插件污染主应用 classpath。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginLoader extends URLClassLoader {

    private final PluginDescriptor descriptor;

    public PluginLoader(PluginDescriptor descriptor, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.descriptor = descriptor;
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
            Class<?> clazz = loadClass(descriptor.className());
            if (!Plugin.class.isAssignableFrom(clazz)) {
                throw new PluginException("插件类未实现 Plugin 接口：" + descriptor.className());
            }
            return (Plugin) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new PluginException("加载插件失败：" + descriptor.id(), e);
        }
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
}
