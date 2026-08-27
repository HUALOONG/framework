package cn.jowen.framework.plugin.loader;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class SharedClassLoaderTest {

    @Test
    void loadClass_fromParent() throws ClassNotFoundException {
        SharedClassLoader cl = new SharedClassLoader(new URL[0], getClass().getClassLoader());
        Class<?> loaded = cl.loadClass("java.lang.String");
        assertThat(loaded).isSameAs(String.class);
    }

    @Test
    void constructor_nullUrls_doesNotThrow() {
        SharedClassLoader cl = new SharedClassLoader(null, getClass().getClassLoader());
        assertThat(cl).isNotNull();
    }

    @Test
    void constructor_emptyUrls() {
        SharedClassLoader cl = new SharedClassLoader(new URL[0], getClass().getClassLoader());
        assertThat(cl).isNotNull();
    }
}
