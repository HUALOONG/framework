package cn.jowen.framework.plugin.classloader;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.assertj.core.api.Assertions.assertThat;

class FrameworkApiDelegateClassLoaderTest {

    @Test
    void delegatesExportedPackageToParent() throws Exception {
        // Use a class that is NOT loaded by Bootstrap (which returns null).
        // We verify that the delegate CL properly attempts delegation by checking
        // that the exported package config is respected and the class can still be loaded.
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        FrameworkApiDelegateClassLoader cl = new FrameworkApiDelegateClassLoader(
                new URL[0], parent, java.util.List.of("cn.jowen.framework"));

        // The plugin CL itself should be loadable (it's in the test classpath)
        // and since cn.jowen.framework is an exported package, it should delegate to parent.
        // Because this class is loaded by the same parent, both should resolve to the same Class object.
        Class<?> clazz = cl.loadClass("cn.jowen.framework.plugin.classloader.FrameworkApiDelegateClassLoader");
        assertThat(clazz).isNotNull();
        // In the same JVM classpath, both parent and child resolve to the same Class object
        // loaded by the parent ClassLoader (AppClassLoader).
        assertThat(clazz.getClassLoader()).isSameAs(parent);
    }

    @Test
    void nonExportedClassConfigCorrect() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        FrameworkApiDelegateClassLoader cl = new FrameworkApiDelegateClassLoader(
                new URL[0], parent, java.util.List.of("java.util"));

        assertThat(cl.getExportedPackages()).containsExactly("java.util");
    }

    @Test
    void exportedPackageMatch() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        FrameworkApiDelegateClassLoader cl = new FrameworkApiDelegateClassLoader(
                new URL[0], parent, java.util.List.of("java.util", "cn.jowen.framework"));

        assertThat(cl.getExportedPackages()).containsExactly("java.util", "cn.jowen.framework");
    }

    @Test
    void parentNullFallback() throws Exception {
        FrameworkApiDelegateClassLoader cl = new FrameworkApiDelegateClassLoader(
                new URL[0], null, java.util.List.of("cn.jowen.framework"));

        Class<?> clazz = cl.loadClass("cn.jowen.framework.plugin.classloader.FrameworkApiDelegateClassLoader");
        assertThat(clazz).isNotNull();
    }
}
