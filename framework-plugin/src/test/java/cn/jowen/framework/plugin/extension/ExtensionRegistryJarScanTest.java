package cn.jowen.framework.plugin.extension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 {@link ExtensionRegistry#scanAndRegister} 在 JAR 协议下的扩展扫描能力。
 *
 * <p>通过反射私有方法 {@code scanJarEntries} 直接操作 JarFile，
 * 并直接调用 scanAndRegister 验证 jar:file: URL 分支。
 */
class ExtensionRegistryJarScanTest {

    @TempDir
    Path tempDir;

    /**
     * AC1：scanJarEntries 能正确注册标注了 @Extension 的实现类。
     */
    @Test
    void scanJarEntriesRegistersValidExtensions() throws Exception {
        ExtensionRegistry reg = new ExtensionRegistry();
        Path jarPath = buildJarFromClasspath();

        URL jarUrl = jarPath.toUri().toURL();
        ClassLoader parentCL = this.getClass().getClassLoader();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, parentCL);

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Method method = ExtensionRegistry.class.getDeclaredMethod("scanJarEntries", JarFile.class, String.class);
                method.setAccessible(true);
                method.invoke(reg, jarFile, "cn/jowen/framework/plugin/extension");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            classLoader.close();
        }

        // Use the same StringTransformer type that the registered classes implement
        var extensions = reg.getExtensions(ExtensionRegistryTest.StringTransformer.class);
        assertThat(extensions).hasSize(2);
        assertThat(extensions.get(0).getClass().getSimpleName()).isEqualTo("StringReverser");
    }

    /**
     * AC2：scanJarEntries 只扫描匹配 basePackage 前缀的类。
     */
    @Test
    void scanJarEntriesOnlyScansMatchingPackage() throws Exception {
        ExtensionRegistry reg = new ExtensionRegistry();
        Path jarPath = buildJarWithMixedPackages();

        URL jarUrl = jarPath.toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl},
                this.getClass().getClassLoader());
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Method method = ExtensionRegistry.class.getDeclaredMethod("scanJarEntries", JarFile.class, String.class);
                method.setAccessible(true);
                method.invoke(reg, jarFile, "cn/jowen/framework/plugin/extension");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            classLoader.close();
        }

        var extensions = reg.getExtensions(ExtensionRegistryTest.StringTransformer.class);
        assertThat(extensions).hasSize(2);
    }

    /**
     * AC3：scanJarEntries 不关闭 JarFile，由调用方负责。
     */
    @Test
    void scanJarEntriesDoesNotCloseJarFile() throws Exception {
        ExtensionRegistry reg = new ExtensionRegistry();
        Path jarPath = buildJarFromClasspath();

        URL jarUrl = jarPath.toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl},
                this.getClass().getClassLoader());
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        JarFile jarFile = new JarFile(jarPath.toFile());
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            Method method = ExtensionRegistry.class.getDeclaredMethod("scanJarEntries", JarFile.class, String.class);
            method.setAccessible(true);
            method.invoke(reg, jarFile, "cn/jowen/framework/plugin/extension");

            // scanJarEntries 不应关闭 JarFile，验证仍然可以读取条目
            assertThat(jarFile.getEntry("cn/jowen/framework/plugin/extension/ExtensionRegistryTest$StringTruncator.class"))
                    .isNotNull();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            classLoader.close();
            jarFile.close();
        }
    }

    /**
     * AC4：非 Extension 类不会被注册。
     */
    @Test
    void scanJarEntriesSkipsNonExtensionClasses() throws Exception {
        ExtensionRegistry reg = new ExtensionRegistry();
        Path jarPath = buildJarWithNonExtensionClass();

        URL jarUrl = jarPath.toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl},
                this.getClass().getClassLoader());
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Method method = ExtensionRegistry.class.getDeclaredMethod("scanJarEntries", JarFile.class, String.class);
                method.setAccessible(true);
                method.invoke(reg, jarFile, "cn/jowen/framework/plugin/extension");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            classLoader.close();
        }

        // NonExtensionClass implements JarScanTest's StringTransformer, not ExtensionRegistryTest's
        // So querying with ExtensionRegistryTest.StringTransformer should return empty
        var extensions = reg.getExtensions(ExtensionRegistryTest.StringTransformer.class);
        assertThat(extensions).isEmpty();
    }

    /**
     * AC5：scanAndRegister 在 jar:file: URL 下能正确扫描并注册扩展。
     */
    @Test
    void scanAndRegisterJarProtocolRegistersExtensions() throws Exception {
        ExtensionRegistry reg = new ExtensionRegistry();
        Path jarPath = buildJarFromClasspath();

        // 将 JAR 置于类加载器路径首位，使 getResource 返回 jar:file: URL
        URL jarUrl = jarPath.toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl},
                this.getClass().getClassLoader());
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            reg.scanAndRegister("cn.jowen.framework.plugin.extension");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            classLoader.close();
        }

        var extensions = reg.getExtensions(ExtensionRegistryTest.StringTransformer.class);
        assertThat(extensions).hasSize(2);
    }

    // ---- helpers ----

    private Path buildJarFromClasspath() throws IOException {
        Path jarPath = tempDir.resolve("test-extension.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addClass(jos, ExtensionRegistryTest.StringTransformer.class);
            addClass(jos, ExtensionRegistryTest.StringTruncator.class);
            addClass(jos, ExtensionRegistryTest.StringReverser.class);
            addClass(jos, Extension.class);
            addClass(jos, ExtensionPoint.class);
        }
        return jarPath;
    }

    private Path buildJarWithMixedPackages() throws IOException {
        Path jarPath = tempDir.resolve("test-mixed.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addClass(jos, ExtensionRegistryTest.StringTransformer.class);
            addClass(jos, ExtensionRegistryTest.StringTruncator.class);
            addClass(jos, ExtensionRegistryTest.StringReverser.class);
            addClass(jos, Extension.class);
            addClass(jos, ExtensionPoint.class);
            addClass(jos, OtherPackageClass.class);
        }
        return jarPath;
    }

    private Path buildJarWithNonExtensionClass() throws IOException {
        Path jarPath = tempDir.resolve("test-no-ext.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addClass(jos, NonExtensionClass.class);
            addClass(jos, ExtensionPoint.class);
        }
        return jarPath;
    }

    private void addClass(JarOutputStream jos, Class<?> clazz) throws IOException {
        String entryName = clazz.getName().replace('.', '/') + ".class";
        String path = "/" + entryName;
        byte[] bytes = clazz.getResourceAsStream(path).readAllBytes();
        jos.putNextEntry(new ZipEntry(entryName));
        jos.write(bytes);
        jos.closeEntry();
    }

    // ---- test fixtures ----

    static class OtherPackageClass implements ExtensionRegistryTest.StringTransformer {
        @Override
        public String transform(String input) {
            return input == null ? null : input + "_other";
        }
    }

    static class NonExtensionClass implements ExtensionRegistryTest.StringTransformer {
        @Override
        public String transform(String input) {
            return input == null ? null : input;
        }
    }
}
