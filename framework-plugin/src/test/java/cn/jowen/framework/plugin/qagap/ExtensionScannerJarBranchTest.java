package cn.jowen.framework.plugin.qagap;

import cn.jowen.framework.plugin.extension.Extension;
import cn.jowen.framework.plugin.extension.ExtensionScanner;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 驱动 {@link ExtensionScanner} 的 {@code jar:} 协议分支与 {@code scanJarEntries}，
 * 覆盖当前 FULL_MISSED 行（jar 分支 41-47 与 scanJarEntries 106-123）。
 *
 * <p>做法：将本测试内 {@link @Extension} 标注的 fixture 类字节码打入临时 jar，
 * 并额外写入一个与包路径同名的目录标记条目，使
 * {@code Thread.currentThread().getContextClassLoader().getResource(包路径)} 能解析为
 * {@code jar:} URL，从而走入 jar 分支而非 file 分支。
 */
class ExtensionScannerJarBranchTest {

    private static final String POINT_ID = "qagap.jar.point";

    @Extension(id = "qagap-jar-impl", extensionPoint = POINT_ID, order = 7)
    public static class JarPointImpl {
    }

    @TempDir
    Path tempDir;

    @Test
    void scan_jarProtocol_registersAnnotatedExtensionFromJar() throws IOException {
        String classFile = JarPointImpl.class.getName().replace('.', '/') + ".class";
        Path jarPath = tempDir.resolve("qagap-fixture.jar");
        try (InputStream in = JarPointImpl.class.getClassLoader().getResourceAsStream(classFile);
             OutputStream out = Files.newOutputStream(jarPath);
             JarOutputStream jar = new JarOutputStream(out)) {
            assertThat(in).isNotNull();
            // 目录标记条目（带尾斜杠）：URLClassLoader 对包路径资源会回退查找 "name/"，
            // 借此让 getResource 解析为 jar: URL，从而走入 jar 分支与 scanJarEntries
            jar.putNextEntry(new ZipEntry("cn/jowen/framework/plugin/qagap/"));
            jar.closeEntry();
            jar.putNextEntry(new ZipEntry(classFile));
            in.transferTo(jar);
            jar.closeEntry();
        }

        ExtensionRegistry registry = new ExtensionRegistry();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{jarPath.toUri().toURL()}, getClass().getClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            new ExtensionScanner(registry).scanAndRegister("cn.jowen.framework.plugin.qagap");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }

        List<Object> extensions = registry.getExtensions(POINT_ID);
        assertThat(extensions).hasSize(1);
        assertThat(registry.getExtensionPointIds()).contains(POINT_ID);
    }

    /**
     * 扫描仅存在于依赖 jar（framework-core.jar）中的包，迫使 {@code getResource}
     * 解析为 {@code jar:} URL，从而走 jar 分支并触发 {@code scanJarEntries}（行 41-47、106-123）。
     */
    @Test
    void scan_dependencyJarPackage_triggersJarScanBranch() {
        ExtensionRegistry registry = new ExtensionRegistry();
        ExtensionScanner scanner = new ExtensionScanner(registry);
        // cn.jowen.framework.core.spi 只存在于 framework-core.jar，无 file: 备选 -> jar 分支
        assertThatCode(() -> scanner.scanAndRegister("cn.jowen.framework.core.spi"))
                .doesNotThrowAnyException();
    }
}
