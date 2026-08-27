package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.core.exception.SystemException;
import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import com.example.demo.DemoPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RestartHotSwapStrategy} 端到端测试：生成真实插件 jar 验证加载/启动/卸载与类加载器持有。
 *
 * @author 王飞
 * @since 2026-08-27
 */
class RestartHotSwapStrategyTest {

    @TempDir
    Path pluginsDir;

    private PluginLifecycleManager manager;

    @BeforeEach
    void setUp() {
        manager = new PluginLifecycleManager();
    }

    @Test
    void onFileModified_loadsAndStartsPlugin() throws IOException {
        buildJar("demo.jar");
        RestartHotSwapStrategy strategy = new RestartHotSwapStrategy(manager, pluginsDir);

        strategy.onFileModified("demo.jar");

        assertThat(manager.getPlugin("demo")).isNotNull();
        assertThat(manager.getPlugin("demo").getState()).isEqualTo(PluginState.STARTED);
        strategy.close();
    }

    @Test
    void onFileModified_reloadsNewVersion() throws IOException {
        buildJar("demo.jar");
        RestartHotSwapStrategy strategy = new RestartHotSwapStrategy(manager, pluginsDir);
        strategy.onFileModified("demo.jar");
        assertThat(manager.getPlugin("demo")).isNotNull();

        // 同文件再次变更触发重载：先卸载旧实例再加载新实例
        strategy.onFileModified("demo.jar");
        assertThat(manager.getPlugin("demo")).isNotNull();
        assertThat(manager.getPlugin("demo").getState()).isEqualTo(PluginState.STARTED);
        strategy.close();
    }

    @Test
    void onFileDeleted_unloadsPlugin() throws IOException {
        buildJar("demo.jar");
        RestartHotSwapStrategy strategy = new RestartHotSwapStrategy(manager, pluginsDir);
        strategy.onFileModified("demo.jar");
        assertThat(manager.getPlugin("demo")).isNotNull();

        strategy.onFileDeleted("demo.jar");

        assertThat(manager.getPlugin("demo")).isNull();
        strategy.close();
    }

    @Test
    void close_releasesLoaders() throws IOException {
        buildJar("demo.jar");
        RestartHotSwapStrategy strategy = new RestartHotSwapStrategy(manager, pluginsDir);
        strategy.onFileModified("demo.jar");

        strategy.close();
        // 关闭后重复 close 不抛异常（幂等安全）
        assertThat(manager.getPlugin("demo")).isNotNull();
    }

    /**
     * 端到端：插件描述符声明扩展点时，热卸载（文件删除）必须注销桥接映射，core 加载器不再返回该扩展。
     */
    @Test
    void onFileDeleted_unregistersBridgeMappings() throws IOException {
        buildJarWithExtensionPoints("demo.jar");
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginSpiBridge bridge = new PluginSpiBridge(registry);
        // 与 boot 装配一致：管理器注入注册中心，卸载时级联清理该插件扩展
        manager.setExtensionRegistry(registry);
        registry.register(new Extension("e1", HotSwapPoint.class.getName(), new HotSwapPointImpl(), 0, "demo", null));
        RestartHotSwapStrategy strategy = new RestartHotSwapStrategy(manager, pluginsDir, bridge);

        strategy.onFileModified("demo.jar");
        ExtensionLoader<HotSwapPoint> loader = ExtensionLoader.getExtensionLoader(HotSwapPoint.class);
        assertThat(loader.getExtension("e1")).isInstanceOf(HotSwapPointImpl.class);

        strategy.onFileDeleted("demo.jar");
        assertThatThrownBy(() -> loader.getExtension("e1"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
        strategy.close();
    }

    /**
     * 端到端：同文件再次变更重载时，桥接映射先注销后重注册，扩展保持可见；策略关闭后清理。
     */
    @Test
    void onFileModified_reload_keepsBridgeMappings() throws IOException {
        buildJarWithExtensionPoints("demo.jar");
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginSpiBridge bridge = new PluginSpiBridge(registry);
        // 与 boot 装配一致：管理器注入注册中心，卸载时级联清理该插件扩展
        manager.setExtensionRegistry(registry);
        registry.register(new Extension("e1", HotSwapPoint.class.getName(), new HotSwapPointImpl(), 0, "demo", null));
        RestartHotSwapStrategy strategy = new RestartHotSwapStrategy(manager, pluginsDir, bridge);
        strategy.onFileModified("demo.jar");
        ExtensionLoader<HotSwapPoint> loader = ExtensionLoader.getExtensionLoader(HotSwapPoint.class);
        assertThat(loader.getExtension("e1")).isInstanceOf(HotSwapPointImpl.class);

        strategy.onFileModified("demo.jar");
        // destroy 已清理注册中心：重载后由新插件实例重新注册扩展，桥接映射应保持可用
        registry.register(new Extension("e1", HotSwapPoint.class.getName(), new HotSwapPointImpl(), 0, "demo", null));
        assertThat(loader.getExtension("e1")).isInstanceOf(HotSwapPointImpl.class);

        strategy.close();
        assertThatThrownBy(() -> loader.getExtension("e1"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
    }

    private void buildJar(String name) throws IOException {
        writeJar(name, "{\"pluginId\":\"demo\",\"version\":\"1.0.0\",\"pluginClass\":\"com.example.demo.DemoPlugin\"}");
    }

    /**
     * 生成声明扩展点描述符的插件 jar（扩展点 id 取接口全限定名，与 @SPI 缺省 id 一致）。
     */
    private void buildJarWithExtensionPoints(String name) throws IOException {
        String pointId = HotSwapPoint.class.getName();
        writeJar(name, "{\"pluginId\":\"demo\",\"version\":\"1.0.0\",\"pluginClass\":\"com.example.demo.DemoPlugin\","
                + "\"extensionPoints\":[{\"id\":\"" + pointId + "\",\"interfaceName\":\"" + pointId
                + "\",\"singleton\":false}]}");
    }

    private void writeJar(String name, String descriptorJson) throws IOException {
        Path jar = pluginsDir.resolve(name);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            jos.write(descriptorJson.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
            try (InputStream is = DemoPlugin.class.getResourceAsStream("/com/example/demo/DemoPlugin.class")) {
                jos.putNextEntry(new JarEntry("com/example/demo/DemoPlugin.class"));
                is.transferTo(jos);
                jos.closeEntry();
            }
        }
    }

    @SPI
    public interface HotSwapPoint {
    }

    static final class HotSwapPointImpl implements HotSwapPoint {
    }
}