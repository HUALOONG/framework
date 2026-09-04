package cn.jowen.framework.plugin.extension;

import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link ExtensionScanner} 测试：覆盖包目录扫描注册、按描述符加载注册及其各分支
 * （未知包、非注解类跳过、抽象类/接口跳过、缺类跳过、重复 id 捕获）。
 *
 * <p>注意：本类与注解 {@link Extension} 同包，故不导入 registry 下的同名 record，
 * 需要其类型时一律使用全限定名，避免遮蔽注解。
 */
class ExtensionScannerTest {

    @Extension(id = "my-ext", extensionPoint = "my.ep")
    public static class MyExtension {
    }

    @Extension(id = "my-abstract", extensionPoint = "my.ep")
    public abstract static class MyAbstractExtension {
    }

    @Extension(id = "my-iface", extensionPoint = "my.ep")
    public interface MyInterfaceExtension {
    }

    public static class NotAnnotated {
    }

    private final ExtensionRegistry registry = new ExtensionRegistry();
    private final ExtensionScanner scanner = new ExtensionScanner(registry);

    private static List<cn.jowen.framework.plugin.registry.Extension> registered(
            ExtensionRegistry registry, String extensionPointId) {
        return registry.getExtensions(extensionPointId);
    }

    @Test
    void scanAndRegister_unknownPackage_returnsSilently() {
        assertThatCode(() -> scanner.scanAndRegister("com.nonexistent.pkg.xyz.abc"))
                .doesNotThrowAnyException();
        assertThat(registry.getExtensionPointIds()).isEmpty();
    }

    @Test
    void scanAndRegister_scansDirectory_registersAnnotated() {
        scanner.scanAndRegister("cn.jowen.framework.plugin.extension.scanpkg");
        List<cn.jowen.framework.plugin.registry.Extension> exts = registered(registry, "scan.ep");
        assertThat(exts).hasSize(1);
        assertThat(exts.get(0).id()).isEqualTo("scan-me");
    }

    @Test
    void registerIfAnnotated_registersAnnotatedClass() throws Exception {
        invokeRegisterIfAnnotated(MyExtension.class);
        List<cn.jowen.framework.plugin.registry.Extension> exts = registered(registry, "my.ep");
        assertThat(exts).hasSize(1);
        assertThat(exts.get(0).id()).isEqualTo("my-ext");
    }

    @Test
    void registerIfAnnotated_skipsNonAnnotated() throws Exception {
        invokeRegisterIfAnnotated(NotAnnotated.class);
        assertThat(registry.getExtensionPointIds()).isEmpty();
    }

    @Test
    void registerIfAnnotated_skipsAbstract() throws Exception {
        invokeRegisterIfAnnotated(MyAbstractExtension.class);
        assertThat(registry.getExtensionPointIds()).isEmpty();
    }

    @Test
    void registerIfAnnotated_skipsInterface() throws Exception {
        invokeRegisterIfAnnotated(MyInterfaceExtension.class);
        assertThat(registry.getExtensionPointIds()).isEmpty();
    }

    @Test
    void loadFromDescriptor_registersInstance() {
        ExtensionDefinition def = new ExtensionDefinition("d1", "ep1", MyExtension.class.getName(), 5, null);
        scanner.loadFromDescriptor(List.of(def), getClass().getClassLoader());

        List<cn.jowen.framework.plugin.registry.Extension> exts = registered(registry, "ep1");
        assertThat(exts).hasSize(1);
        assertThat(exts.get(0).id()).isEqualTo("d1");
    }

    @Test
    void loadFromDescriptor_missingClass_skips() {
        ExtensionDefinition def = new ExtensionDefinition("d2", "ep2", "com.missing.ClassX", 1, null);
        assertThatCode(() -> scanner.loadFromDescriptor(List.of(def), getClass().getClassLoader()))
                .doesNotThrowAnyException();
        assertThat(registered(registry, "ep2")).isEmpty();
    }

    @Test
    void loadFromDescriptor_duplicateId_isCaughtAndSkipped() {
        ExtensionDefinition def1 = new ExtensionDefinition("dup", "ep3", MyExtension.class.getName(), 1, null);
        ExtensionDefinition def2 = new ExtensionDefinition("dup", "ep3", MyExtension.class.getName(), 2, null);
        assertThatCode(() -> scanner.loadFromDescriptor(List.of(def1, def2), getClass().getClassLoader()))
                .doesNotThrowAnyException();
        // 第二个重复 id 注册抛 IllegalArgumentException，被 loadFromDescriptor 的 catch 吞掉
        assertThat(registered(registry, "ep3")).hasSize(1);
    }

    private void invokeRegisterIfAnnotated(Class<?> clazz) throws Exception {
        Method m = ExtensionScanner.class.getDeclaredMethod("registerIfAnnotated", Class.class);
        m.setAccessible(true);
        m.invoke(scanner, clazz);
    }
}
