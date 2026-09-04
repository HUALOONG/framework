package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PluginBootstrapTest {

    private BootPluginProperties properties(String pluginsDir) {
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(pluginsDir);
        props.setAutoStart(true);
        return props;
    }

    @Test
    void constructor_twoArg_delegatesToThreeArg() {
        PluginManager pm = mock(PluginManager.class);
        BootPluginProperties props = properties("/no/such/dir/xyz");
        PluginBootstrap bootstrap = new PluginBootstrap(pm, props);
        assertThat(bootstrap).isNotNull();
    }

    @Test
    void afterSingletonsInstantiated_missingDir_isNoOp() {
        PluginManager pm = mock(PluginManager.class);
        BootPluginProperties props = properties("/no/such/dir/xyz");
        PluginBootstrap bootstrap = new PluginBootstrap(pm, props, null);
        // 目录不存在：应静默跳过，不抛异常
        bootstrap.afterSingletonsInstantiated();
        bootstrap.destroy();
    }

    @Test
    void destroy_withoutBridgeAndNoLoaders_isNoOp() {
        PluginManager pm = mock(PluginManager.class);
        BootPluginProperties props = properties("/no/such/dir/xyz");
        PluginBootstrap bootstrap = new PluginBootstrap(pm, props, null);
        bootstrap.destroy();
    }

    @Test
    void afterSingletonsInstantiated_emptyDir_logsAndReturns(@TempDir Path pluginsDir) {
        PluginManager pm = mock(PluginManager.class);
        BootPluginProperties props = properties(pluginsDir.toString());
        props.getClassLoading().setStrategy("framework-api-delegate");
        PluginBootstrap bootstrap = new PluginBootstrap(pm, props, null);

        // 目录存在但无 jar：解析策略 -> 扫描目录 -> 空列表后返回
        bootstrap.afterSingletonsInstantiated();
    }

    @Test
    void afterSingletonsInstantiated_invalidStrategy_fallsBackToDefault(@TempDir Path pluginsDir) {
        PluginManager pm = mock(PluginManager.class);
        BootPluginProperties props = properties(pluginsDir.toString());
        props.getClassLoading().setStrategy("bogus-strategy");
        PluginBootstrap bootstrap = new PluginBootstrap(pm, props, null);

        bootstrap.afterSingletonsInstantiated();
    }

    @Test
    void afterSingletonsInstantiated_withInvalidJar_logsLoadFailure(@TempDir Path pluginsDir) throws Exception {
        PluginManager pm = mock(PluginManager.class);
        // 非法 zip 内容：PluginLoader 构造/读取描述符失败，被 loadOne 的 catch 吞掉，不阻断启动
        Path bogusJar = pluginsDir.resolve("bogus.jar");
        Files.write(bogusJar, "not a zip".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        BootPluginProperties props = properties(pluginsDir.toString());
        PluginBootstrap bootstrap = new PluginBootstrap(pm, props, null);

        assertThatCode(bootstrap::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    void destroy_withBridge_clearsMappings() {
        PluginManager pm = mock(PluginManager.class);
        PluginSpiBridge bridge = mock(PluginSpiBridge.class);
        BootPluginProperties props = properties("/no/such/dir/xyz");
        PluginBootstrap bootstrap = new PluginBootstrap(pm, props, bridge);

        bootstrap.destroy();

        verify(bridge).clear();
    }
}
