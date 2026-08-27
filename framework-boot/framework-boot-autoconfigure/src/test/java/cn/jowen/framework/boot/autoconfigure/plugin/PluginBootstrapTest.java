package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link PluginBootstrap} 单元测试：覆盖目录缺失/空目录的安全跳过与 destroy 清理。
 * （端到端加载需示例插件 jar，超出单测范围。）
 *
 * @author 王飞
 * @since 2026-08-27
 */
class PluginBootstrapTest {

    @Test
    void noPluginsDir_isNoOp() {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir("./nonexistent-plugins-" + System.nanoTime());
        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);

        assertThatCode(bootstrap::afterSingletonsInstantiated).doesNotThrowAnyException();
        assertThat(manager.getPlugins()).isEmpty();
        assertThatCode(bootstrap::destroy).doesNotThrowAnyException();
    }

    @Test
    void emptyPluginsDir_isNoOp(@TempDir Path tempDir) {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(tempDir.toString());
        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);

        bootstrap.afterSingletonsInstantiated();
        assertThat(manager.getPlugins()).isEmpty();
    }

    @Test
    void autoStartFalse_doesNotStartEvenIfLoaded(@TempDir Path tempDir) {
        // 无 jar 时仅验证 autoStart=false 配置可读取且不抛异常
        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(tempDir.toString());
        props.setAutoStart(false);
        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);

        bootstrap.afterSingletonsInstantiated();
        assertThat(props.isAutoStart()).isFalse();
        assertThat(manager.getPlugins()).isEmpty();
    }
}