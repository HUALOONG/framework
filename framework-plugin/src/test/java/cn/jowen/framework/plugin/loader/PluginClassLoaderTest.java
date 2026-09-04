package cn.jowen.framework.plugin.loader;

import com.example.demo.DemoPlugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginClassLoaderTest {

    @Test
    void exportedPackages_defaultEmpty() {
        TestPluginClassLoader cl = new TestPluginClassLoader(new URL[0],
                getClass().getClassLoader(), List.of(), List.of(), List.of());
        assertThat(cl.getExportedPackages()).isEmpty();
        assertThat(cl.getImportedPackages()).isEmpty();
        assertThat(cl.getHiddenClasses()).isEmpty();
    }

    @Test
    void exportedPackages_withPackages() {
        List<String> exported = List.of("cn.jowen.framework");
        TestPluginClassLoader cl = new TestPluginClassLoader(new URL[0],
                getClass().getClassLoader(), exported, List.of(), List.of());
        assertThat(cl.getExportedPackages()).containsExactly("cn.jowen.framework");
    }

    @Test
    void hiddenClass_throwsClassNotFoundException() {
        TestPluginClassLoader cl = new TestPluginClassLoader(new URL[0],
                getClass().getClassLoader(), List.of(), List.of(), List.of("javax.servlet"));
        assertThatThrownBy(() -> cl.loadClass("javax.servlet.http.HttpServletRequest"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("禁止加载");
    }

    @Test
    void hiddenClass_subPackage_alsoBlocked() {
        TestPluginClassLoader cl = new TestPluginClassLoader(new URL[0],
                getClass().getClassLoader(), List.of(), List.of(), List.of("javax"));
        assertThatThrownBy(() -> cl.loadClass("javax.servlet.http.HttpServletRequest"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("禁止加载");
    }

    @Test
    void nonHiddenClass_delegatesToParent() throws ClassNotFoundException {
        TestPluginClassLoader cl = new TestPluginClassLoader(new URL[0],
                getClass().getClassLoader(), List.of(), List.of(), List.of("hidden.pkg"));
        Class<?> loaded = cl.loadClass("java.lang.String");
        assertThat(loaded).isSameAs(String.class);
    }

    @Test
    void constructor_nullLists_defaultsToEmpty() {
        TestPluginClassLoader cl = new TestPluginClassLoader(new URL[0],
                getClass().getClassLoader(), null, null, null);
        assertThat(cl.getExportedPackages()).isEmpty();
        assertThat(cl.getImportedPackages()).isEmpty();
        assertThat(cl.getHiddenClasses()).isEmpty();
    }

    static class TestPluginClassLoader extends PluginClassLoader {
        TestPluginClassLoader(URL[] urls, ClassLoader parent,
                              List<String> exported, List<String> imported, List<String> hidden) {
            super(urls, parent, exported, imported, hidden);
        }

        @Override
        protected Class<?> doLoadClass(String name, boolean resolve) throws ClassNotFoundException {
            return getParent().loadClass(name);
        }
    }

    @Test
    void getResource_fallsThroughToFindResource(@TempDir Path dir) throws IOException {
        Path res = dir.resolve("res.txt");
        Files.writeString(res, "hello");
        URL url = res.getParent().toUri().toURL();
        TestPluginClassLoader cl = new TestPluginClassLoader(new URL[]{url},
                getClass().getClassLoader(), List.of(), List.of(), List.of());
        assertThat(cl.getResource("res.txt")).isNotNull();
        assertThat(cl.getResource("missing.txt")).isNull();
    }

    /** 自行定义类的加载器，用于覆盖已加载缓存与 resolve 分支。 */
    static final class SelfDefiningClassLoader extends PluginClassLoader {
        SelfDefiningClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent, List.of(), List.of(), List.of());
        }

        @Override
        protected Class<?> doLoadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                Class<?> c = findClass(name);
                if (resolve) resolveClass(c);
                return c;
            } catch (ClassNotFoundException e) {
                return getParent().loadClass(name);
            }
        }
    }

    @Test
    void loadClass_cachedAndResolve() throws Exception {
        URL url = DemoPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI().toURL();
        SelfDefiningClassLoader cl = new SelfDefiningClassLoader(new URL[]{url}, getClass().getClassLoader());
        Class<?> first = cl.loadClass("com.example.demo.DemoPlugin");
        Class<?> second = cl.loadClass("com.example.demo.DemoPlugin");
        assertThat(second).isSameAs(first);
        // 第二次命中 findLoadedClass 缓存；resolve=true 触发 resolveClass 分支
        Class<?> resolved = cl.loadClass("com.example.demo.DemoPlugin", true);
        assertThat(resolved).isSameAs(first);
    }
}
