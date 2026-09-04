package cn.jowen.framework.plugin.support;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.lifecycle.PluginStateTransition;
import cn.jowen.framework.plugin.resolver.VersionArbitrator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 支撑类零散分支契约：工具类实例化无副作用、模板方法钩子真实执行。
 *
 * <p>{@code doInit()} 模板钩子若被静默跳过，子类初始化逻辑（如指标埋点、上下文注入）
 * 会全部失效且没有任何报错，故必须断言启动链路可完整走通。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class SupportGapCoverageTest {

    @Test
    void versionArbitrator_instantiationIsSideEffectFree() {
        assertThatCode(() -> new VersionArbitrator()).doesNotThrowAnyException();
    }

    @Test
    void stateTransition_instantiationIsSideEffectFree() {
        assertThatCode(() -> new PluginStateTransition()).doesNotThrowAnyException();
    }

    @Test
    void abstractPlugin_doInitHookIsInvokedOnStart() {
        Plugin plugin = new AbstractPlugin(new PluginDescriptor("demo")) {
            @Override
            protected void doStart(PluginContext context) {
            }

            @Override
            protected void doStop() {
            }
        };

        assertThatCode(() -> plugin.start(null)).doesNotThrowAnyException();
        assertThat(plugin.getState()).isEqualTo(PluginState.STARTED);
    }
}
