package cn.jowen.framework.plugin.loader;

import org.junit.jupiter.api.Test;

import java.net.URL;
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
}
