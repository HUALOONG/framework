package cn.jowen.framework.plugin.classloader;

import org.jspecify.annotations.NullMarked;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.io.IOException;

/**
 * 完全隔离类加载器（child-first）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class IsolatedClassLoader extends PluginClassLoader {

    public IsolatedClassLoader(URL[] urls) {
        super(urls, null, Collections.emptyList());
    }

    public IsolatedClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent, Collections.emptyList());
    }

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
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }

    /** T3：资源查找不向上委派父加载器，仅查找自身 URL。 */
    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    /** T3：资源查找枚举不向上委派父加载器，仅查找自身 URL。 */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }
}
