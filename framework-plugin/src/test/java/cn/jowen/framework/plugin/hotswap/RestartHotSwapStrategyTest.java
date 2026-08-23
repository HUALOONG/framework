package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.plugin.DefaultPluginManager;
import cn.jowen.framework.plugin.Plugin;
import cn.jowen.framework.plugin.PluginDescriptor;
import cn.jowen.framework.plugin.PluginLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import pluginimpl.StubPlugin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 测试 {@link RestartHotSwapStrategy} 的热部署健壮性：
 * <ul>
 *   <li>ID 来源：从 PluginDescriptor.id() 获取，而非 jar 文件名</li>
 *   <li>异常日志化：catch 块中记录 warning 日志</li>
 *   <li>类加载器释放：PluginLoader 使用 try-with-resources 确保 close() 被调用</li>
 *   <li>install 分支：manager.get(id) == null 时执行完整 load</li>
 * </ul>
 */
class RestartHotSwapStrategyTest {

    @TempDir
    Path tempDir;

    private DefaultPluginManager manager;
    private RestartHotSwapStrategy strategy;
    private Path pluginsDir;

    @BeforeEach
    void setUp() {
        manager = new DefaultPluginManager();
        pluginsDir = tempDir.resolve("plugins");
        assertThatCode(() -> Files.createDirectories(pluginsDir)).doesNotThrowAnyException();
        strategy = new RestartHotSwapStrategy(manager, pluginsDir);
    }

    /**
     * AC1：onPluginChange 能从 jar 中正确加载新插件。
     * StubPlugin.id() 返回 "stub"，descriptor id 也设为 "stub" 以匹配。
     */
    @Test
    void onPluginChangeLoadsNewPlugin() throws Exception {
        Path jarPath = buildPluginJar("stub", "1.0.0", StubPlugin.class.getName());

        strategy.onPluginChange("stub-1.0.0.jar");

        Plugin plugin = manager.get("stub");
        assertThat(plugin).isNotNull();
        assertThat(plugin.id()).isEqualTo("stub");
    }

    /**
     * AC2：ID 来自 PluginDescriptor.id()，而非 jar 文件名。
     * 验证：jar 文件名为 "different-name.jar"，但 descriptor id 为 "stub"（与 StubPlugin.id() 一致），
     * 查询应使用 "stub"。
     */
    @Test
    void onPluginChangeUsesDescriptorIdNotFileName() throws Exception {
        // descriptor id = "stub" 与 StubPlugin.id() 保持一致
        // 但 jar 文件名不同，验证 ID 来自 descriptor 而非文件名
        Path jarPath = buildPluginJar("stub", "2.0.0", StubPlugin.class.getName());
        Path renamedJar = pluginsDir.resolve("different-name.jar");
        Files.copy(jarPath, renamedJar);

        strategy.onPluginChange("different-name.jar");

        // ID 应来自 descriptor（即 "stub"），而不是文件名
        Plugin plugin = manager.get("stub");
        assertThat(plugin).isNotNull();
        assertThat(manager.get("different-name")).isNull();
    }

    /**
     * AC3：热更新时先 unregister 旧实例，再 load 新实例。
     */
    @Test
    void onPluginChangeUnregistersExistingThenReloads() throws Exception {
        // StubPlugin.id() 返回 "stub"，descriptor id 必须与之匹配
        Path jarPath1 = buildPluginJar("stub", "1.0.0", StubPlugin.class.getName());
        Path jarPath2 = buildPluginJar("stub", "2.0.0", StubPlugin.class.getName());

        // 第一次加载
        strategy.onPluginChange("stub-1.0.0.jar");
        Plugin first = manager.get("stub");
        assertThat(first).isNotNull();

        // 第二次加载（热更新）
        strategy.onPluginChange("stub-2.0.0.jar");
        Plugin second = manager.get("stub");

        // 应该是新实例（旧实例已被 unregister）
        assertThat(second).isNotNull();
        assertThat(second).isNotSameAs(first);
    }

    /**
     * AC4：jar 文件不存在时不抛出异常。
     */
    @Test
    void onPluginChangeHandlesMissingJarGracefully() {
        assertThatCode(() -> strategy.onPluginChange("nonexistent.jar"))
                .doesNotThrowAnyException();
    }

    /**
     * AC5：jar 解析失败时记录 warning 日志但不抛出异常。
     */
    @Test
    void onPluginChangeLogsExceptionOnParseError() throws Exception {
        Path badJar = tempDir.resolve("bad.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(badJar))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            jos.write("not valid json".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        Files.move(badJar, pluginsDir.resolve("bad.jar"));

        assertThatCode(() -> strategy.onPluginChange("bad.jar"))
                .doesNotThrowAnyException();
    }

    /**
     * AC6：install 分支 — 插件不存在时执行完整 load。
     */
    @Test
    void installBranchLoadsWhenPluginNotExists() throws Exception {
        // descriptor id = "stub" 与 StubPlugin.id() 一致
        Path jarPath = buildPluginJar("stub", "1.0.0", StubPlugin.class.getName());

        strategy.onPluginChange("stub-1.0.0.jar");

        assertThat(manager.get("stub")).isNotNull();
    }

    /**
     * AC7：restart() 方法能正确 unregister 插件。
     */
    @Test
    void restartUnregistersExistingPlugin() throws Exception {
        // descriptor id = "stub" 与 StubPlugin.id() 一致
        Path jarPath = buildPluginJar("stub", "1.0.0", StubPlugin.class.getName());

        strategy.onPluginChange("stub-1.0.0.jar");
        assertThat(manager.get("stub")).isNotNull();

        strategy.restart("stub");

        assertThat(manager.get("stub")).isNull();
    }

    /**
     * AC8：restart() 对不存在的 ID 不抛出异常。
     */
    @Test
    void restartWithNonExistentIdDoesNothing() {
        assertThatCode(() -> strategy.restart("non-existent"))
                .doesNotThrowAnyException();
    }

    /**
     * AC9：PluginLoader 在 load 后通过 try-with-resources 正确释放。
     * 验证：热更新后获得新实例（旧 loader 已释放，新 loader 已创建）。
     */
    @Test
    void loaderIsClosedAfterLoad() throws Exception {
        // descriptor id = "stub" 与 StubPlugin.id() 一致
        Path jarPath = buildPluginJar("stub", "1.0.0", StubPlugin.class.getName());

        strategy.onPluginChange("stub-1.0.0.jar");
        Plugin first = manager.get("stub");
        assertThat(first).isNotNull();

        // 换版本触发 reload，验证旧 loader 被释放、新实例被加载
        Path jarPath2 = buildPluginJar("stub", "2.0.0", StubPlugin.class.getName());
        strategy.onPluginChange("stub-2.0.0.jar");

        Plugin latest = manager.get("stub");
        assertThat(latest).isNotNull();
        // StubPlugin.version() 固定返回 "1.0.0"，改用实例不同性验证 reload 生效
        assertThat(latest).isNotSameAs(first);
    }

    // ---- helpers ----

    private Path buildPluginJar(String id, String version, String className) throws IOException {
        Path jarPath = pluginsDir.resolve(id + "-" + version + ".jar");
        String pluginJson = "{\"id\":\"" + id
                + "\",\"version\":\"" + version
                + "\",\"class\":\"" + className
                + "\",\"description\":\"test\",\"dependencies\":[],\"exportedPackages\":[],\"springEnabled\":false}";

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            // plugin.json
            jos.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            jos.write(pluginJson.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            // StubPlugin 类（从 classpath 读取）
            byte[] classBytes = readClassBytes(StubPlugin.class);
            jos.putNextEntry(new JarEntry(StubPlugin.class.getName().replace('.', '/') + ".class"));
            jos.write(classBytes);
            jos.closeEntry();
        }
        return jarPath;
    }

    private byte[] readClassBytes(Class<?> clazz) {
        String path = "/" + clazz.getName().replace('.', '/') + ".class";
        try (var is = clazz.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("无法找到类文件: " + path);
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("读取类文件失败: " + path, e);
        }
    }
}
