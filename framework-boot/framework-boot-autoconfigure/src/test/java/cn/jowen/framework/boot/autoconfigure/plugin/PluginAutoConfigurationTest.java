package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.hotswap.HotSwapStrategy;
import cn.jowen.framework.plugin.hotswap.PluginHotSwapManager;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import cn.jowen.framework.plugin.resolver.DependencyResolver;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link PluginAutoConfiguration} 装配测试：默认上下文注册桥接门面与引导器，
 * 并覆盖私有 {@code parseStrategy}/{@code parseDebounceMillis} 解析逻辑与热部署条件分支。
 */
class PluginAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class));

    @Test
    void defaultContext_registersSpiBridgeAndBootstrap() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(PluginSpiBridge.class);
            assertThat(ctx).hasSingleBean(PluginBootstrap.class);
            // 热部署缺省未开启（matchIfMissing=false）时不应注册管理器 bean
            assertThat(ctx).doesNotHaveBean(PluginHotSwapManager.class);
        });
    }

    @Test
    void parseStrategy_resolvesValidAndFallsBackToRestart() throws Exception {
        Method m = PluginAutoConfiguration.class.getDeclaredMethod("parseStrategy", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(null, "RESTART")).isEqualTo(HotSwapStrategy.RESTART);
        assertThat(m.invoke(null, "reload_classes")).isEqualTo(HotSwapStrategy.RELOAD_CLASSES);
        assertThat(m.invoke(null, "MANUAL")).isEqualTo(HotSwapStrategy.MANUAL);
        // 非法值回退 RESTART
        assertThat(m.invoke(null, "bogus")).isEqualTo(HotSwapStrategy.RESTART);
    }

    @Test
    void parseDebounceMillis_parsesUnitsAndFallsBackTo3000() throws Exception {
        Method m = PluginAutoConfiguration.class.getDeclaredMethod("parseDebounceMillis", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(null, "500ms")).isEqualTo(500L);
        assertThat(m.invoke(null, "2s")).isEqualTo(2000L);
        assertThat(m.invoke(null, "1500")).isEqualTo(1500L);
        assertThat(m.invoke(null, "  3s  ")).isEqualTo(3000L); // 去空格 + 小写归一
        assertThat(m.invoke(null, "abc")).isEqualTo(3000L);     // 解析失败回退 3000
    }

    @Test
    void pluginLifecycleManager_withoutExtensionRegistry_setsNoRegistry() {
        PluginAutoConfiguration config = new PluginAutoConfiguration();
        PluginLifecycleManager manager = config.pluginLifecycleManager(nullProvider(ExtensionRegistry.class));
        assertThat(manager).isNotNull();
        // 未注入注册中心时扩展查询返回空
        assertThat(manager.getExtension("any")).isNull();
    }

    @Test
    void hotSwapEnabled_registersStartedHotSwapManager(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        context.withPropertyValues(
                        "framework.plugin.hot-swap.enabled=true",
                        "framework.plugin.plugins-dir=" + dir)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    // 热部署开关开启且 PluginManager 由 pluginLifecycleManager 提供时应注册已启动的管理器
                    assertThat(ctx).hasSingleBean(PluginHotSwapManager.class);
                });
    }

    @Test
    void pluginExtensionRegistry_and_DependencyResolver_registeredByDefault() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(ExtensionRegistry.class);
            assertThat(ctx).hasSingleBean(DependencyResolver.class);
        });
    }

    @Test
    void pluginLifecycleManager_withExtensionRegistry_setsRegistry() {
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginAutoConfiguration config = new PluginAutoConfiguration();
        PluginLifecycleManager manager = config.pluginLifecycleManager(
                ofProvider(ExtensionRegistry.class, registry));
        assertThat(manager).isNotNull();
        assertThat(manager.getExtension("any")).isNull();
    }

    @Test
    void pluginSpiBridge_withExtensionRegistry_bridges() {
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginAutoConfiguration config = new PluginAutoConfiguration();
        PluginSpiBridge bridge = config.pluginSpiBridge(ofProvider(ExtensionRegistry.class, registry));
        assertThat(bridge).isNotNull();
    }

    @Test
    void pluginSpiBridge_withoutExtensionRegistry_returnsNull() {
        PluginAutoConfiguration config = new PluginAutoConfiguration();
        assertThat(config.pluginSpiBridge(nullProvider(ExtensionRegistry.class))).isNull();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> nullProvider(Class<T> type) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> ofProvider(Class<T> type, T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}