package cn.jowen.framework.plugin.loader;

import org.jspecify.annotations.NullMarked;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * 插件类加载器抽象基类，封装 {@link URLClassLoader} 提供统一的加载行为与资源查找能力。
 *
 * @author 王飞
 */
@NullMarked
public abstract class PluginClassLoader extends URLClassLoader {

    private final List<String> exportedPackages;
    private final List<String> importedPackages;
    private final List<String> hiddenClasses;

    protected PluginClassLoader(URL[] urls, ClassLoader parent,
                                List<String> exportedPackages,
                                List<String> importedPackages,
                                List<String> hiddenClasses) {
        super(urls, parent);
        this.exportedPackages = exportedPackages == null ? List.of() : List.copyOf(exportedPackages);
        this.importedPackages = importedPackages == null ? List.of() : List.copyOf(importedPackages);
        this.hiddenClasses = hiddenClasses == null ? List.of() : List.copyOf(hiddenClasses);
    }

    private static String packageNameOf(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    public List<String> getExportedPackages() {
        return exportedPackages;
    }

    public List<String> getImportedPackages() {
        return importedPackages;
    }

    public List<String> getHiddenClasses() {
        return hiddenClasses;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    /**
     * 重载带 {@code resolve} 参数的类加载入口，交由子类决定是否优先从自身 URL 查找。
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 1. 已加载的类直接返回
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            if (resolve) resolveClass(c);
            return c;
        }
        // 2. 隐藏类禁止加载
        if (isHiddenClass(name)) {
            throw new ClassNotFoundException("禁止加载的类：" + name);
        }
        return doLoadClass(name, resolve);
    }

    /**
     * 子类实现具体加载逻辑。
     */
    protected abstract Class<?> doLoadClass(String name, boolean resolve) throws ClassNotFoundException;

    private boolean isHiddenClass(String className) {
        String pkg = packageNameOf(className);
        for (String hidden : hiddenClasses) {
            if (pkg.equals(hidden) || pkg.startsWith(hidden + ".")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (url != null) return url;
        return findResource(name);
    }
}
