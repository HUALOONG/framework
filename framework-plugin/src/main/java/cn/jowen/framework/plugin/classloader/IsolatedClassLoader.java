package cn.jowen.framework.plugin.classloader;

import org.jspecify.annotations.NullMarked;

import java.net.URL;
import java.util.Collections;

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
}
