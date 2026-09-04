package cn.jowen.framework.plugin.extension;

import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link ExtensionScanner} jar 协议扫描测试：把本测试的字节码打进临时 jar，
 * 以 {@code jar:} URL 走 scanJarEntries 分支；同时覆盖描述符加载的失败容错。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ExtensionScannerJarTest {

    /** 测试扩展点 id。 */
    private static final String POINT_ID = "jar.point.test";

    /** 打进 jar 的 fixture：标注 @Extension，可被扫描注册。 */
    @Extension(id = "jar-impl", extensionPoint = POINT_ID, order = 5)
    public static class JarPointImpl {
    }

    @TempDir
    Path tempDir;

    /**
     * 把 fixture 类的字节码写入临时 jar 并返回路径。
     */
    private Path packFixtureJar() throws IOException {
        String classFile = JarPointImpl.class.getName().replace('.', '/') + ".class";
        Path jarPath = tempDir.resolve("fixture.jar");
        try (InputStream in = JarPointImpl.class.getClassLoader().getResourceAsStream(classFile);
             OutputStream out = Files.newOutputStream(jarPath);
             JarOutputStream jar = new JarOutputStream(out)) {
            assertThat(in).isNotNull();
            jar.putNextEntry(new ZipEntry(classFile));
            in.transferTo(jar);
            jar.closeEntry();
        }
        return jarPath;
    }

    @Test
    void scan_jarProtocol_registersAnnotatedExtension() throws IOException {
        Path jarPath = packFixtureJar();
        ExtensionRegistry registry = new ExtensionRegistry();

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(
                new java.net.URL[] {jarPath.toUri().toURL()},
                ExtensionScannerJarTest.class.getClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            new ExtensionScanner(registry).scanAndRegister(JarPointImpl.class.getPackageName());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }

        List<Object> extensions = registry.getExtensions(POINT_ID);
        assertThat(extensions).hasSize(1);
        assertThat(registry.getExtensionPointIds()).contains(POINT_ID);
    }

    @Test
    void loadFromDescriptor_missingClass_skipsWithoutThrowing() {
        ExtensionRegistry registry = new ExtensionRegistry();
        ExtensionDefinition definition = new ExtensionDefinition(
                "ghost", POINT_ID, "cn.jowen.framework.plugin.NonexistentImpl", 0, null);

        ExtensionScanner scanner = new ExtensionScanner(registry);
        assertThatCode(() -> scanner.loadFromDescriptor(List.of(definition), getClass().getClassLoader()))
                .doesNotThrowAnyException();
        assertThat(registry.getExtensions(POINT_ID)).isEmpty();
    }

    @Test
    void loadFromDescriptor_realClass_registersExtension() {
        ExtensionRegistry registry = new ExtensionRegistry();
        ExtensionDefinition definition = new ExtensionDefinition(
                "real", POINT_ID, JarPointImpl.class.getName(), 3, null);

        ExtensionScanner scanner = new ExtensionScanner(registry);
        scanner.loadFromDescriptor(List.of(definition), getClass().getClassLoader());

        assertThat(registry.getExtensions(POINT_ID)).hasSize(1);
        assertThat(registry.getExtensionPointIds()).contains(POINT_ID);
    }

    @Test
    void scanAndRegister_unknownPackage_isNoOp() {
        ExtensionRegistry registry = new ExtensionRegistry();
        new ExtensionScanner(registry).scanAndRegister("cn.jowen.framework.plugin.nonexistent.pkg");
        assertThat(registry.getExtensionPointIds()).isEmpty();
    }

    @Test
    void scan_directoryProtocol_registersConcreteAndSkipsInterfaceAndAbstract() {
        ExtensionRegistry registry = new ExtensionRegistry();
        new ExtensionScanner(registry).scanAndRegister("cn.jowen.framework.plugin.extension.dirscan");

        // 具体类（含嵌套目录递归）被注册；接口与抽象类被跳过
        List<Object> exts = registry.getExtensions("dir.point");
        assertThat(exts).hasSize(2);
        assertThat(registry.getExtensionPointIds()).contains("dir.point");
    }
}
