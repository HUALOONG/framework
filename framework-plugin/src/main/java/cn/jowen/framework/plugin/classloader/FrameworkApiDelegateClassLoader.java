package cn.jowen.framework.plugin.classloader;

import org.jspecify.annotations.NullMarked;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * 框架 API 委派类加载器。
 *
 * <p>根据 {@link PluginClassLoader#getExportedPackages()} 配置，将指定包的类请求
 * 委派给父加载器，其余类仍从插件 URL 查找，实现 API 共享与插件私有隔离并存。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class FrameworkApiDelegateClassLoader extends PluginClassLoader {

    public FrameworkApiDelegateClassLoader(URL[] urls, ClassLoader parent, List<String> exportedPackages) {
        super(urls, parent, exportedPackages);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (matchesExportedPackage(name)) {
            if (getParent() != null) {
                return getParent().loadClass(name);
            }
            return ClassLoader.getSystemClassLoader().loadClass(name);
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public URL getResource(String name) {
        if (matchesExportedPackage(name)) {
            if (getParent() != null) {
                URL url = getParent().getResource(name);
                if (url != null) return url;
            }
            URL sysUrl = ClassLoader.getSystemClassLoader().getResource(name);
            if (sysUrl != null) return sysUrl;
        }
        return super.getResource(name);
    }

    private boolean matchesExportedPackage(String className) {
        String pkg = packageNameOf(className);
        for (String exported : getExportedPackages()) {
            if (pkg.equals(exported) || pkg.startsWith(exported + ".")) {
                return true;
            }
        }
        return false;
    }

    private static String packageNameOf(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
}
