package cn.jowen.framework.plugin.classloader;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;
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

    /** T3：getResource 不向上委派，仅查找自身 URL。 */
    @Test
    void getResourceDelegatesOnlyToOwnUrls() throws Exception {
        // 父加载器可找到 java 标准类资源；isolated 加载器应完全隔离，返回 null
        URLClassLoader parent = new URLClassLoader(new URL[0], getClass().getClassLoader());
        try (IsolatedClassLoader loader = new IsolatedClassLoader(new URL[0], parent)) {
            // 父加载器能找到的资源，isolated 加载器找不到
            assertThat(loader.getResource("java/util/ArrayList.class")).isNull();
            // 自身也不持有的资源同样找不到
            assertThat(loader.getResource("some/nonexistent.txt")).isNull();
        }
    }

    /** T3：getResources 不向上委派，仅返回自身 URL 中的资源。 */
    @Test
    void getResourcesDelegatesOnlyToOwnUrls() throws Exception {
        URLClassLoader parent = new URLClassLoader(new URL[0], getClass().getClassLoader());
        try (IsolatedClassLoader loader = new IsolatedClassLoader(new URL[0], parent)) {
            Enumeration<URL> resources = loader.getResources("java/util/ArrayList.class");
            assertThat(resources).isNotNull();
            // isolated 不应返回父加载器的资源
            long parentCount = java.util.Collections.list(resources).size();
            assertThat(parentCount).isEqualTo(0);
        }
    }
}
