package cn.jowen.framework.plugin.classloader;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IsolatedClassLoaderTest {

    @Test
    void doesNotDelegateToParent() throws Exception {
        // 父加载器持有某些类，但隔离加载器采用 child-first 且不委托父加载器，
        // 因此仅由父/启动加载器提供的类应抛 ClassNotFoundException。
        URLClassLoader parent = new URLClassLoader(new URL[0], getClass().getClassLoader());
        IsolatedClassLoader loader = new IsolatedClassLoader(new URL[0], parent);

        assertThatThrownBy(() -> loader.loadClass("java.util.ArrayList"))
                .isInstanceOf(ClassNotFoundException.class);
        loader.close();
    }
}
