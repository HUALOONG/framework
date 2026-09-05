package cn.jowen.framework.plugin.qagap;

import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.plugin.extension.ExtensionScanner;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link ExtensionScanner} 目录扫描补充覆盖：
 * <ul>
 *   <li>扫描 classpath 上真实存在的包（file: 协议），对每个可加载类调用 registerIfAnnotated
 *       （覆盖 line 86 附近的可加载分支）；</li>
 *   <li>直接驱动私有 scanDirectory 扫描含“不可加载 .class”的临时目录，触发 ClassNotFoundException
 *       的 catch 分支（覆盖 line 88-89）。</li>
 * </ul>
 */
class ExtensionScannerDirectoryTest {

    @Test
    void scan_realPackage_invokesRegisterIfAnnotated() {
        ExtensionRegistry registry = new ExtensionRegistry();
        ExtensionScanner scanner = new ExtensionScanner(registry);
        // 扫描生产包（file: 协议），目录中存在可加载的 .class，
        // 每个类都会进入 registerIfAnnotated；包内无 @Extension 类，故不注册
        assertThatCode(() -> scanner.scanAndRegister("cn.jowen.framework.plugin.api"))
                .doesNotThrowAnyException();
    }

    @Test
    void scanDirectory_unloadableClass_catchesClassNotFound(@TempDir Path tmpDir) throws Exception {
        // 顶层放置 Broken.class（文件名 Broken.class -> 类名 broken.Broken，
        // 与 URLClassLoader 根下的实际位置不匹配，Class.forName 抛 ClassNotFoundException）
        Path broken = tmpDir.resolve("Broken.class");
        Files.write(broken, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0x00});

        URLClassLoader cl = new URLClassLoader(new URL[]{tmpDir.toUri().toURL()}, null);
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            ExtensionRegistry registry = new ExtensionRegistry();
            ExtensionScanner scanner = new ExtensionScanner(registry);
            // 直接驱动私有方法：扫描 tmpDir 下的 Broken.class，
            // Class.forName("broken.Broken") 在 cl 中无法解析 -> ClassNotFoundException
            // -> 被 scanDirectory 内部 catch 记录（line 88-89），不向外传播
            assertThatCode(() -> ReflectionUtils.invokeMethod(scanner, "scanDirectory", tmpDir, "broken"))
                    .doesNotThrowAnyException();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
