package cn.jowen.framework.plugin.classloader;

import org.jspecify.annotations.NullMarked;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 插件类加载器抽象基类，封装 {@link URLClassLoader} 提供统一的加载行为与资源查找能力。
 *
 * <p>子类应实现 {@link #loadClass(String)} 与可选的父加载器委派策略。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public abstract class PluginClassLoader extends URLClassLoader {

    /** 扩展点包名称列表（用于委派策略）。 */
    private final java.util.List<String> exportedPackages;

    protected PluginClassLoader(URL[] urls, ClassLoader parent, java.util.List<String> exportedPackages) {
        super(urls, parent);
        this.exportedPackages = exportedPackages == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(exportedPackages);
    }

    /** @return 已配置的导出包列表 */
    public java.util.List<String> getExportedPackages() {
        return exportedPackages;
    }

    /**
     * 重载类加载入口，交由子类决定是否优先从自身 URL 查找。
     *
     * @param name 类名，不可为 {@code null}
     * @return 对应的 {@link Class} 对象
     * @throws ClassNotFoundException 找不到类时抛出
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    /**
     * 重载带 {@code resolve} 参数的类加载入口。
     *
     * @param name   类名，不可为 {@code null}
     * @param resolve 是否在加载后解析
     * @return 对应的 {@link Class} 对象
     * @throws ClassNotFoundException 找不到类时抛出
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            if (resolve) resolveClass(c);
            return c;
        }
        try {
            c = findClass(name);
            if (resolve) resolveClass(c);
            return c;
        } catch (ClassNotFoundException ignored) {
            // fall through to parent
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (url != null) {
            return url;
        }
        return findResource(name);
    }
}
