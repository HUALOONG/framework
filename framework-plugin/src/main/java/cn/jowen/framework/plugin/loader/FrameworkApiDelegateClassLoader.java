package cn.jowen.framework.plugin.loader;

import org.jspecify.annotations.NullMarked;

import java.net.URL;
import java.util.List;

/**
 * 框架 API 委派类加载器。
 *
 * <p>根据配置的导出包列表，将框架 API 包和插件 API 包请求委派给父加载器，
 * 其余类仍从插件 URL 查找，实现 API 共享与插件私有隔离并存。
 *
 * @author 王飞
 */
@NullMarked
public final class FrameworkApiDelegateClassLoader extends PluginClassLoader {

    private static final List<String> DEFAULT_EXPORTED = List.of(
            "cn.jowen.framework.core",
            "cn.jowen.framework.plugin.api"
    );

    public FrameworkApiDelegateClassLoader(URL[] urls, ClassLoader parent, List<String> exportedPackages) {
        super(urls, parent,
                mergedExportedPackages(exportedPackages),
                List.of(), List.of());
    }

    private static List<String> mergedExportedPackages(List<String> userPackages) {
        List<String> merged = new java.util.ArrayList<>(DEFAULT_EXPORTED);
        if (userPackages != null) merged.addAll(userPackages);
        return List.copyOf(merged);
    }

    @Override
    protected Class<?> doLoadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 1. 先尝试自身加载（打破双亲委派）
        try {
            Class<?> c = findClass(name);
            if (resolve) resolveClass(c);
            return c;
        } catch (ClassNotFoundException ignored) {
            // fall through
        }
        // 2. 兜底委派父加载器
        if (getParent() != null) {
            return getParent().loadClass(name);
        }
        return ClassLoader.getSystemClassLoader().loadClass(name);
    }
}
