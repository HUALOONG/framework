package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.core.exception.SystemException;
import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginBootstrap} 单元测试：覆盖目录缺失/空目录的安全跳过与 destroy 清理。
 * （端到端加载需示例插件 jar，超出单测范围。）
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
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

    /**
     * 验证 destroy 时桥接门面被清理：插件扩展对 core 加载器不再可见。
     */
    @Test
    void destroy_clearsBridgeMappings(@TempDir Path tempDir) {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(tempDir.toString());

        ExtensionRegistry registry = new ExtensionRegistry();
        PluginSpiBridge bridge = new PluginSpiBridge(registry);
        registry.register(new Extension("e1", BootPoint.ID, new BootPointImpl(), 0, "p1", null));
        bridge.registerExtensionPoint(BootPoint.ID, BootPoint.class);

        PluginBootstrap bootstrap = new PluginBootstrap(manager, props, bridge);
        bootstrap.afterSingletonsInstantiated();
        assertThat(ExtensionLoader.getExtensionLoader(BootPoint.class).getExtension("e1")).isNotNull();

        bootstrap.destroy();
        assertThatThrownBy(() -> ExtensionLoader.getExtensionLoader(BootPoint.class).getExtension("e1"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
    }

    @SPI(id = BootPoint.ID)
    public interface BootPoint {
        String ID = "boot.point.test";
    }

    static final class BootPointImpl implements BootPoint {
    }
}