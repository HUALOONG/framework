package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginBootstrap} 分支覆盖测试：坏 jar 容错、非 jar 文件忽略、
 * 非法加载策略回退与重复销毁（端到端插件加载见 {@link PluginBootstrapTest}）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class PluginBootstrapBranchesTest {

    @TempDir
    Path tempDir;

    @Test
    void constructor_rejectsNullArguments() {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();

        assertThatThrownBy(() -> new PluginBootstrap(null, props))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PluginBootstrap(manager, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PluginBootstrap(null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void brokenJar_skippedWithoutFailure() throws IOException {
        Path jar = tempDir.resolve("broken-plugin.jar");
        Files.write(jar, "not a zip archive".getBytes());

        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(tempDir.toString());
        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);

        // 单插件失败不阻断启动
        assertThatCode(bootstrap::afterSingletonsInstantiated).doesNotThrowAnyException();
        assertThat(manager.getPlugins()).isEmpty();
    }

    @Test
    void nonJarFiles_ignored() throws IOException {
        Files.write(tempDir.resolve("readme.txt"), "hello".getBytes());
        Files.write(tempDir.resolve("plugin.json"), "{}".getBytes());

        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(tempDir.toString());
        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);

        assertThatCode(bootstrap::afterSingletonsInstantiated).doesNotThrowAnyException();
        assertThat(manager.getPlugins()).isEmpty();
    }

    @Test
    void invalidClassLoadingStrategy_fallsBackToDefault() throws IOException {
        Path jar = tempDir.resolve("broken-plugin.jar");
        Files.write(jar, "not a zip archive".getBytes());

        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(tempDir.toString());
        props.getClassLoading().setStrategy("nonsense-strategy");
        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);

        // 非法策略回退默认值后继续走坏 jar 容错分支，不抛异常
        assertThatCode(bootstrap::afterSingletonsInstantiated).doesNotThrowAnyException();
        assertThat(manager.getPlugins()).isEmpty();
    }

    @Test
    void kebabCaseStrategy_parsedWithoutThrowing() throws IOException {
        Path jar = tempDir.resolve("broken-plugin.jar");
        Files.write(jar, "not a zip archive".getBytes());

        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(tempDir.toString());
        props.getClassLoading().setStrategy("parent-first");
        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);

        assertThatCode(bootstrap::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    void destroy_isIdempotent() {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(tempDir.toString());
        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);

        assertThatCode(bootstrap::destroy).doesNotThrowAnyException();
        assertThatCode(bootstrap::destroy).doesNotThrowAnyException();
    }
}
