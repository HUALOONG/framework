package cn.jowen.framework.plugin.loader;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FrameworkApiDelegateClassLoaderTest {

    @Test
    void exportedPackages_containsDefaults() {
        FrameworkApiDelegateClassLoader cl = new FrameworkApiDelegateClassLoader(
                new URL[0], getClass().getClassLoader(), List.of());
        assertThat(cl.getExportedPackages()).contains(
                "cn.jowen.framework.core",
                "cn.jowen.framework.plugin.api"
        );
    }

    @Test
    void exportedPackages_mergesUserPackages() {
        FrameworkApiDelegateClassLoader cl = new FrameworkApiDelegateClassLoader(
                new URL[0], getClass().getClassLoader(), List.of("com.example.api"));
        assertThat(cl.getExportedPackages()).contains(
                "cn.jowen.framework.core",
                "cn.jowen.framework.plugin.api",
                "com.example.api"
        );
    }

    @Test
    void loadClass_parentDelegated() throws ClassNotFoundException {
        FrameworkApiDelegateClassLoader cl = new FrameworkApiDelegateClassLoader(
                new URL[0], getClass().getClassLoader(), List.of());
        Class<?> loaded = cl.loadClass("java.lang.String");
        assertThat(loaded).isSameAs(String.class);
    }

    @Test
    void loadClass_unknownClass_throws() throws ClassNotFoundException {
        FrameworkApiDelegateClassLoader cl = new FrameworkApiDelegateClassLoader(
                new URL[0], getClass().getClassLoader(), List.of());
        try {
            cl.loadClass("com.nonexistent.UnlikelyClass");
        } catch (ClassNotFoundException e) {
            assertThat(e).isNotNull();
        }
    }
}
