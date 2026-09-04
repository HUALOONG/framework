package cn.jowen.framework.plugin.context;

import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.event.PluginEvent;
import cn.jowen.framework.plugin.event.PluginEventListener;
import cn.jowen.framework.plugin.event.PluginStartedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DefaultPluginContextTest {

    private DefaultPluginContext ctx;
    private final PluginDescriptor descriptor = new PluginDescriptor("test");
    private final SharedData sharedData = new SharedData();
    private final PluginConfiguration config = new SimpleConfiguration();
    private final ClassLoader loader = getClass().getClassLoader();

    @BeforeEach
    void setUp() {
        ctx = new DefaultPluginContext("test", descriptor, config, sharedData, null, loader, loader, null);
    }

    @Test
    void getPluginId() {
        assertThat(ctx.getPluginId()).isEqualTo("test");
    }

    @Test
    void getPluginDescriptor() {
        assertThat(ctx.getPluginDescriptor()).isSameAs(descriptor);
    }

    @Test
    void getConfiguration() {
        assertThat(ctx.getConfiguration()).isSameAs(config);
    }

    @Test
    void getSharedData() {
        assertThat(ctx.getSharedData()).isSameAs(sharedData);
    }

    @Test
    void getApplicationClassLoader() {
        assertThat(ctx.getApplicationClassLoader()).isSameAs(loader);
    }

    @Test
    void getPluginClassLoader() {
        assertThat(ctx.getPluginClassLoader()).isSameAs(loader);
    }

    @Test
    void getSpringContext_null() {
        assertThat(ctx.getSpringContext()).isNull();
    }

    @Test
    void getPluginManager_returnsConfiguredManager() {
        PluginManager manager = mock(PluginManager.class);
        DefaultPluginContext withManager = new DefaultPluginContext(
                "test", descriptor, config, sharedData, manager, loader, loader, null);
        assertThat(withManager.getPluginManager()).isSameAs(manager);
    }

    @Test
    void publishEvent_listenerExceptionIsCaught() {
        PluginEventListener bad = e -> {
            throw new RuntimeException("boom");
        };
        ctx.addListener(bad);
        // 监听器异常不应向上传播
        ctx.publishEvent(new PluginStartedEvent("test"));
    }

    @Test
    void publishEvent_notifiesListener() {
        java.util.concurrent.atomic.AtomicReference<PluginEvent> captured = new java.util.concurrent.atomic.AtomicReference<>();
        ctx.addListener(captured::set);
        ctx.publishEvent(new PluginStartedEvent("test"));
        assertThat(captured.get()).isNotNull();
    }

    @Test
    void publishEvent_nullIgnored() {
        ctx.publishEvent(null);
    }

    @Test
    void getState_initialCreated() {
        assertThat(ctx.getState()).isEqualTo(PluginState.CREATED);
    }

    @Test
    void setState_validTransition() {
        ctx.setState(PluginState.STARTING);
        assertThat(ctx.getState()).isEqualTo(PluginState.STARTING);
    }

    @Test
    void setState_invalidTransition_throws() {
        assertThatThrownBy(() -> ctx.setState(PluginState.STARTED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addListener_multipleListeners() {
        java.util.List<PluginEvent> events = new java.util.ArrayList<>();
        ctx.addListener(events::add);
        ctx.addListener(events::add);
        ctx.publishEvent(new PluginStartedEvent("test"));
        assertThat(events).hasSize(2);
    }

    @Test
    void close_clearsListeners() {
        java.util.concurrent.atomic.AtomicReference<PluginEvent> captured = new java.util.concurrent.atomic.AtomicReference<>();
        ctx.addListener(captured::set);
        ctx.close();
        ctx.publishEvent(new PluginStartedEvent("test"));
        assertThat(captured.get()).isNull();
    }

    @Test
    void close_closesSpringContext() {
        AutoCloseableSpring spring = new AutoCloseableSpring();
        DefaultPluginContext withSpring = new DefaultPluginContext("test", descriptor, config, sharedData, null, loader, loader, spring);
        withSpring.close();
        assertThat(spring.closed).isTrue();
    }

    static class AutoCloseableSpring implements AutoCloseable {
        boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    void close_fromStopping_setsStopped() {
        // 合法路径 CREATED -> STARTING -> STARTED -> STOPPING，
        // 使 close() 内的 setState(STOPPED) 转换成功（而非被非法转换吞掉）
        ctx.setState(PluginState.STARTING);
        ctx.setState(PluginState.STARTED);
        ctx.setState(PluginState.STOPPING);
        ctx.close();
        assertThat(ctx.getState()).isEqualTo(PluginState.STOPPED);
    }

    static class SimpleConfiguration implements PluginConfiguration {
        public String getString(String key) { return ""; }
        public int getInt(String key, int defaultValue) { return defaultValue; }
        public boolean getBoolean(String key, boolean defaultValue) { return defaultValue; }
        public java.util.List<String> getList(String key) { return java.util.List.of(); }
        public java.util.Map<String, String> getMap(String key) { return java.util.Map.of(); }
        public java.util.Map<String, Object> getAll() { return java.util.Map.of(); }
    }
}
