package cn.jowen.framework.plugin.spi;

import cn.jowen.framework.core.exception.SystemException;
import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.plugin.descriptor.ExtensionPointDescriptor;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginSpiBridge} 测试：插件扩展经桥接对 core {@link ExtensionLoader} 可见。
 */
class PluginSpiBridgeTest {

    @SPI(id = "spi.filter")
    public interface Filter {
        String id();
    }

    @SPI
    public interface PlainPoint {
    }

    public interface NotSpi {
    }

    static final class FilterImpl implements Filter {
        @Override
        public String id() {
            return "filter";
        }
    }

    private ExtensionRegistry registry;
    private PluginSpiBridge bridge;

    @BeforeEach
    void setUp() {
        registry = new ExtensionRegistry();
        bridge = new PluginSpiBridge(registry);
    }

    @AfterEach
    void cleanup() {
        // 移除本用例注册的桥接源，避免污染 JVM 级加载器单例
        bridge.clear();
    }

    @Test
    void register_extensionVisibleThroughCoreLoader() {
        registry.register(new Extension("filter-a", "spi.filter", new FilterImpl(), 0, "p1", null));
        bridge.registerExtensionPoint("spi.filter", Filter.class);

        ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
        assertThat(loader.getExtension("filter-a")).isInstanceOf(FilterImpl.class);
    }

    @Test
    void hotRegisterAndUnregister_reflectsLive() {
        bridge.registerExtensionPoint("spi.filter", Filter.class);
        ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);

        // 运行期热注册：缓存失效后立即可见
        registry.register(new Extension("hot", "spi.filter", new FilterImpl(), 0, "p1", null));
        assertThat(loader.getExtension("hot")).isInstanceOf(FilterImpl.class);

        // 热注销后不可见
        registry.unregister("spi.filter", "hot");
        assertThatThrownBy(() -> loader.getExtension("hot"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
    }

    @Test
    void idMismatchWithSpiId_ignored() {
        registry.register(new Extension("a", "wrong.id", new FilterImpl(), 0, "p1", null));
        // 声明 id 与 @SPI(id="spi.filter") 不一致 → 拒绝桥接
        bridge.registerExtensionPoint("wrong.id", Filter.class);

        ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
        assertThatThrownBy(() -> loader.getExtension("a")).isInstanceOf(SystemException.class);
        assertThat(bridge.unregisterExtensionPoint("wrong.id")).isFalse();
    }

    @Test
    void defaultId_isInterfaceFqn() {
        bridge.registerExtensionPoint(PlainPoint.class.getName(), PlainPoint.class);
        assertThat(bridge.unregisterExtensionPoint(PlainPoint.class.getName())).isTrue();
    }

    @Test
    void register_defaultFqnId_thenExtensionVisible() {
        registry.register(new Extension("pp", PlainPoint.class.getName(), new PlainPointImpl(), 0, "p1", null));
        bridge.registerExtensionPoint(PlainPoint.class.getName(), PlainPoint.class);
        assertThat(ExtensionLoader.getExtensionLoader(PlainPoint.class).getExtension("pp"))
                .isInstanceOf(PlainPointImpl.class);
    }

    @Test
    void nonInterface_ignored() {
        bridge.registerExtensionPoint("not.interface", FilterImpl.class);
        assertThat(bridge.unregisterExtensionPoint("not.interface")).isFalse();
    }

    @Test
    void nonSpiInterface_ignored() {
        bridge.registerExtensionPoint(NotSpi.class.getName(), NotSpi.class);
        assertThat(bridge.unregisterExtensionPoint(NotSpi.class.getName())).isFalse();
    }

    @Test
    void descriptor_interfaceNotFound_ignored() {
        bridge.registerExtensionPoint(new ExtensionPointDescriptor("p", "com.example.NotExists", false));
        assertThat(bridge.unregisterExtensionPoint("p")).isFalse();
    }

    @Test
    void descriptor_registersMapping() {
        bridge.registerExtensionPoint(new ExtensionPointDescriptor("spi.filter", Filter.class.getName(), false));
        assertThat(bridge.unregisterExtensionPoint("spi.filter")).isTrue();
    }

    @Test
    void unregister_extensionNoLongerVisible() {
        registry.register(new Extension("f", "spi.filter", new FilterImpl(), 0, "p1", null));
        bridge.registerExtensionPoint("spi.filter", Filter.class);
        ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
        assertThat(loader.getExtension("f")).isNotNull();

        assertThat(bridge.unregisterExtensionPoint("spi.filter")).isTrue();
        assertThatThrownBy(() -> loader.getExtension("f"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
    }

    @Test
    void clear_removesAll() {
        bridge.registerExtensionPoint("spi.filter", Filter.class);
        bridge.registerExtensionPoint(PlainPoint.class.getName(), PlainPoint.class);
        bridge.clear();
        assertThat(bridge.unregisterExtensionPoint("spi.filter")).isFalse();
        assertThat(bridge.unregisterExtensionPoint(PlainPoint.class.getName())).isFalse();
    }

    @Test
    void nullArguments_throw() {
        assertThatThrownBy(() -> new PluginSpiBridge(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bridge.registerExtensionPoint(null, Filter.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bridge.registerExtensionPoint("x", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bridge.unregisterExtensionPoint(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** PlainPoint 的无状态实现。 */
    static final class PlainPointImpl implements PlainPoint {
    }

    @SPI
    public interface SharedPoint {
    }

    static final class SharedPointImpl implements SharedPoint {
    }

    /**
     * 多插件共享扩展点：任一插件注销不拆除共享源，全部注销后源才移除。
     */
    @Test
    void sharedPoint_unregisterOneKeepsSourceUntilLastRemoved() {
        ExtensionPointDescriptor point = new ExtensionPointDescriptor(
                SharedPoint.class.getName(), SharedPoint.class.getName(), false);
        registry.register(new Extension("a-ext", SharedPoint.class.getName(), new SharedPointImpl(), 0, "plugin-a", null));
        bridge.registerPlugin("plugin-a", List.of(point));
        bridge.registerPlugin("plugin-b", List.of(point));
        ExtensionLoader<SharedPoint> loader = ExtensionLoader.getExtensionLoader(SharedPoint.class);
        assertThat(loader.getExtension("a-ext")).isInstanceOf(SharedPointImpl.class);

        // 插件 A 卸载：扩展点仍被 B 引用，共享源保留
        bridge.unregisterPlugin("plugin-a");
        assertThat(loader.getExtension("a-ext")).isNotNull();

        // 插件 B 卸载：引用归零，源移除
        bridge.unregisterPlugin("plugin-b");
        assertThatThrownBy(() -> loader.getExtension("a-ext"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
    }

    @Test
    void unregisterPlugin_unknownPlugin_noOp() {
        bridge.unregisterPlugin("nope");
    }

    @Test
    void duplicateBareRegister_needsMatchingUnregisters() {
        bridge.registerExtensionPoint(PlainPoint.class.getName(), PlainPoint.class);
        bridge.registerExtensionPoint(PlainPoint.class.getName(), PlainPoint.class);
        // 仍有一个引用，共享源保留
        assertThat(bridge.unregisterExtensionPoint(PlainPoint.class.getName())).isFalse();
        assertThat(bridge.unregisterExtensionPoint(PlainPoint.class.getName())).isTrue();
    }
}