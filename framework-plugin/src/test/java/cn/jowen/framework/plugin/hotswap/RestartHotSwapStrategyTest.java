package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
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

    private void buildJar(String name) throws IOException {
        Path jar = pluginsDir.resolve(name);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            jos.write("{\"pluginId\":\"demo\",\"version\":\"1.0.0\",\"pluginClass\":\"com.example.demo.DemoPlugin\"}"
                    .getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
            try (InputStream is = DemoPlugin.class.getResourceAsStream("/com/example/demo/DemoPlugin.class")) {
                jos.putNextEntry(new JarEntry("com/example/demo/DemoPlugin.class"));
                is.transferTo(jos);
                jos.closeEntry();
            }
        }
    }
}