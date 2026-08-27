package cn.jowen.framework.plugin.hotswap;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginHotSwapManager} 测试：关闭时清理桥接映射。
 */
class PluginHotSwapManagerTest {

    @SPI
    public interface Point {
    }

    static final class PointImpl implements Point {
    }

    @Test
    void close_clearsBridgeMappings(@TempDir Path tempDir) {
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginSpiBridge bridge = new PluginSpiBridge(registry);
        registry.register(new Extension("e1", Point.class.getName(), new PointImpl(), 0, "p1", null));
        bridge.registerExtensionPoint(Point.class.getName(), Point.class);
        ExtensionLoader<Point> loader = ExtensionLoader.getExtensionLoader(Point.class);
        assertThat(loader.getExtension("e1")).isInstanceOf(PointImpl.class);

        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.RESTART, 100, bridge);

        manager.close();

        assertThatThrownBy(() -> loader.getExtension("e1"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
    }
}