package cn.jowen.framework.plugin.lifecycle;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.event.PluginEvent;
import cn.jowen.framework.plugin.event.PluginEventListener;
import cn.jowen.framework.plugin.event.PluginFailedEvent;
import cn.jowen.framework.plugin.event.PluginStartedEvent;
import cn.jowen.framework.plugin.event.PluginStartingEvent;
import cn.jowen.framework.plugin.event.PluginStoppedEvent;
import cn.jowen.framework.plugin.event.PluginStoppingEvent;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginLifecycleManagerTest {

    @Mock
    Plugin plugin;
    @Mock
    PluginContext context;
    @Mock
    ExtensionRegistry extRegistry;

    private PluginLifecycleManager manager;

    @BeforeEach
    void setup() {
        lenient().when(plugin.getDescriptor()).thenReturn(PluginDescriptor.of("p1", "1.0", "c.P"));
        lenient().when(plugin.getState()).thenReturn(PluginState.CREATED);
        lenient().when(context.getPluginId()).thenReturn("p1");
        lenient().when(context.getPluginDescriptor()).thenReturn(PluginDescriptor.of("p1", "1.0", "c.P"));
        manager = new PluginLifecycleManager();
    }

    @Test
    void initialize_setsStartingState() {
        manager.initialize("p1", plugin, context);
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STARTING);
        assertThat(manager.getPlugin("p1")).isSameAs(plugin);
        assertThat(manager.getContext("p1")).isSameAs(context);
    }

    @Test
    void start_transitionsToStarted() {
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        verify(plugin).start(context);
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STARTED);
    }

    @Test
    void start_missingPlugin_isNoop() {
        manager.start("ghost");
    }

    @Test
    void start_failure_transitionsToFailedAndThrows() {
        manager.initialize("p1", plugin, context);
        doThrow(new RuntimeException("boom")).when(plugin).start(any());
        assertThatThrownBy(() -> manager.start("p1")).isInstanceOf(RuntimeException.class);
        assertThat(manager.getState("p1")).isEqualTo(PluginState.FAILED);
    }

    @Test
    void stop_transitionsToStopped() {
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        manager.stop("p1");
        verify(plugin).stop();
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STOPPED);
    }

    @Test
    void restart_callsStartTwice() {
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        manager.restart("p1");
        verify(plugin, times(2)).start(context);
    }

    @Test
    void destroy_removesRegistration() {
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        manager.destroy("p1");
        assertThat(manager.getPlugin("p1")).isNull();
        assertThat(manager.getState("p1")).isEqualTo(PluginState.CREATED);
    }

    @Test
    void destroy_withExtensionRegistry_unregistersPlugin() {
        manager.setExtensionRegistry(extRegistry);
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        manager.destroy("p1");
        verify(extRegistry).unregisterPlugin("p1");
    }

    @Test
    void events_publishedToListener() {
        List<PluginEvent> events = new ArrayList<>();
        PluginEventListener listener = events::add;
        manager.addListener(listener);
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        assertThat(events).anyMatch(e -> e instanceof PluginStartingEvent);
        assertThat(events).anyMatch(e -> e instanceof PluginStartedEvent);
    }

    @Test
    void getPlugins_and_getPluginsByState() {
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        assertThat(manager.getPlugins()).contains(plugin);
        assertThat(manager.getPluginsByState(PluginState.STARTED)).contains(plugin);
    }

    @Test
    void getExtension_withoutRegistry_returnsNull() {
        assertThat(manager.getExtension("ep")).isNull();
        assertThat(manager.getExtensions(Runnable.class)).isEmpty();
    }

    @Test
    void getExtension_withRegistry_delegates() {
        Extension ext = new Extension("ext1", "ep", new Object(), 0, "p1", null);
        when(extRegistry.getExtensions("ep")).thenReturn(List.of(ext));
        manager.setExtensionRegistry(extRegistry);
        assertThat(manager.getExtension("ep")).isEqualTo(ext);
    }

    @Test
    void pluginManagerDelegates() {
        // 按状态机合法路径依次调用各委派方法：
        // STARTING->STARTED->STOPPING->STOPPED->(destroy)->未注册
        // 注意：restartPlugin 内部先调 stop 再调 start；STOPPED→STARTING 非法，故用独立插件演示 restart
        manager.initialize("p1", plugin, context);
        manager.startPlugin("p1");
        manager.stopPlugin("p1");
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STOPPED);

        // 独立插件：restart 的完整生命周期
        Plugin p2 = mock(Plugin.class);
        manager.initialize("p2", p2, context);
        manager.startPlugin("p2");
        manager.restartPlugin("p2");
        assertThat(manager.getState("p2")).isEqualTo(PluginState.STARTED);

        manager.destroy("p1");
        assertThat(manager.getPlugin("p1")).isNull();
        manager.destroy("p2");
        assertThat(manager.getPlugin("p2")).isNull();
    }

    @Test
    void loadPlugins_unsupported() {
        assertThatThrownBy(() -> manager.loadPlugins(Path.of(".")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> manager.loadPlugin(Path.of("x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void stop_missingPlugin_isNoop() {
        // 未注册插件直接 stop：静默返回
        manager.stop("ghost");
        assertThat(manager.getState("ghost")).isEqualTo(PluginState.CREATED);
    }

    @Test
    void stop_fromStartingState_skipsStoppingTransition() {
        List<PluginEvent> events = new ArrayList<>();
        manager.addListener(events::add);
        // initialize 后状态为 STARTING：stop 跳过 STOPPING 转换，
        // 但 STARTING -> STOPPED 非法，最终转入 FAILED 并包装抛出
        manager.initialize("p1", plugin, context);

        assertThatThrownBy(() -> manager.stop("p1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("插件停止失败");
        verify(plugin).stop();
        assertThat(manager.getState("p1")).isEqualTo(PluginState.FAILED);
        assertThat(events).noneMatch(e -> e instanceof PluginStoppingEvent);
    }

    @Test
    void stop_failure_transitionsToFailedAndThrows() {
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        doThrow(new RuntimeException("stop boom")).when(plugin).stop();

        assertThatThrownBy(() -> manager.stop("p1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("插件停止失败");
        assertThat(manager.getState("p1")).isEqualTo(PluginState.FAILED);
    }

    @Test
    void publish_listenerException_isSwallowed() {
        PluginEventListener bad = event -> {
            throw new RuntimeException("listener boom");
        };
        manager.addListener(bad);
        // 监听异常不应向上冒泡
        manager.initialize("p1", plugin, context);
        manager.start("p1");
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STARTED);
    }

    @Test
    void getState_unknownPlugin_returnsCreated() {
        assertThat(manager.getState("ghost")).isEqualTo(PluginState.CREATED);
    }

    @Test
    void getExtension_withRegistry_emptyReturnsNull() {
        when(extRegistry.getExtensions("ep")).thenReturn(List.of());
        when(extRegistry.getExtensionsByType(Runnable.class)).thenReturn(List.of());
        manager.setExtensionRegistry(extRegistry);

        assertThat(manager.getExtension("ep")).isNull();
        assertThat(manager.getExtensions(Runnable.class)).isEmpty();
    }

    @Test
    void healthCheckers_and_registerExtensionPoint() {
        PluginHealthChecker checker =
                p -> PluginHealthChecker.HealthResult.ok(PluginState.STARTED);
        manager.addHealthChecker(checker);
        assertThat(manager.getHealthCheckers()).containsExactly(checker);
        // 扩展点描述符由加载链注册，管理器侧为空实现
        manager.registerExtensionPoint(null);
        assertThat(manager.getHealthCheckers()).containsExactly(checker);
    }
}
