package cn.jowen.framework.plugin.context;

import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginBeanPostProcessorTest {

    private PluginBeanPostProcessor processor;
    private TestPluginContext ctx;

    @BeforeEach
    void setUp() {
        processor = new PluginBeanPostProcessor();
        ctx = new TestPluginContext();
    }

    @Test
    void postProcessBeforeInitialization_injectsContext() {
        ContextAwareBean bean = new ContextAwareBean();
        processor.postProcessBeforeInitialization(bean, "myBean", ctx);
        assertThat(bean.getContext()).isSameAs(ctx);
    }

    @Test
    void postProcessBeforeInitialization_nonAwareBean_noOp() {
        Object bean = new Object();
        processor.postProcessBeforeInitialization(bean, "myBean", ctx);
    }

    @Test
    void postProcessAfterInitialization_injectsConfig() {
        ConfigTargetBean bean = new ConfigTargetBean();
        processor.postProcessAfterInitialization(bean, ctx);
        assertThat(bean.getConfig()).isNotNull();
    }

    @Test
    void postProcessAfterInitialization_injectsSharedData() {
        DataTargetBean bean = new DataTargetBean();
        processor.postProcessAfterInitialization(bean, ctx);
        assertThat(bean.getData()).isSameAs(ctx.getSharedData());
    }

    @Test
    void postProcessAfterInitialization_nonTargetBean_noOp() {
        Object bean = new Object();
        processor.postProcessAfterInitialization(bean, ctx);
    }

    static class ContextAwareBean implements PluginBeanPostProcessor.PluginContextAware {
        private PluginContext ctx;
        public void setPluginContext(PluginContext ctx) { this.ctx = ctx; }
        public PluginContext getContext() { return ctx; }
    }

    static class ConfigTargetBean implements PluginBeanPostProcessor.PluginConfigurationTarget {
        private cn.jowen.framework.plugin.context.PluginConfiguration cfg;
        public void setConfiguration(cn.jowen.framework.plugin.context.PluginConfiguration cfg) { this.cfg = cfg; }
        public cn.jowen.framework.plugin.context.PluginConfiguration getConfig() { return cfg; }
    }

    static class DataTargetBean implements PluginBeanPostProcessor.SharedDataTarget {
        private SharedData data;
        public void setSharedData(SharedData data) { this.data = data; }
        public SharedData getData() { return data; }
    }

    static class TestPluginContext implements PluginContext {
        private final SharedData sharedData = new SharedData();
        private final cn.jowen.framework.plugin.context.PluginConfiguration config = new cn.jowen.framework.plugin.context.PluginConfiguration() {
            public String getString(String key) { return ""; }
            public int getInt(String key, int defaultValue) { return defaultValue; }
            public boolean getBoolean(String key, boolean defaultValue) { return defaultValue; }
            public java.util.List<String> getList(String key) { return java.util.List.of(); }
            public java.util.Map<String, String> getMap(String key) { return java.util.Map.of(); }
            public java.util.Map<String, Object> getAll() { return java.util.Map.of(); }
        };

        public String getPluginId() { return "test"; }
        public cn.jowen.framework.plugin.descriptor.PluginDescriptor getPluginDescriptor() {
            return new cn.jowen.framework.plugin.descriptor.PluginDescriptor("test");
        }
        public cn.jowen.framework.plugin.context.PluginConfiguration getConfiguration() { return config; }
        public SharedData getSharedData() { return sharedData; }
        public cn.jowen.framework.plugin.api.PluginManager getPluginManager() { return null; }
        public ClassLoader getApplicationClassLoader() { return getClass().getClassLoader(); }
        public ClassLoader getPluginClassLoader() { return getClass().getClassLoader(); }
        public Object getSpringContext() { return null; }
        public void publishEvent(cn.jowen.framework.plugin.event.PluginEvent event) {}
        public void close() {}
    }
}
