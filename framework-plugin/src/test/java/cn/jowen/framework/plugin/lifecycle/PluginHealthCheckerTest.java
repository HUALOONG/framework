package cn.jowen.framework.plugin.lifecycle;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginHealthCheckerTest {

    @Test
    void HealthResult_ok_returnsHealthy() {
        PluginHealthChecker.HealthResult result = PluginHealthChecker.HealthResult.ok(PluginState.STARTED);
        assertThat(result.healthy()).isTrue();
        assertThat(result.message()).isEqualTo("healthy");
        assertThat(result.state()).isEqualTo(PluginState.STARTED);
    }

    @Test
    void HealthResult_fail_returnsUnhealthy() {
        PluginHealthChecker.HealthResult result =
                PluginHealthChecker.HealthResult.fail(PluginState.FAILED, "启动超时");
        assertThat(result.healthy()).isFalse();
        assertThat(result.message()).isEqualTo("启动超时");
        assertThat(result.state()).isEqualTo(PluginState.FAILED);
    }

    @Test
    void HealthResult_equalsAndHashcode() {
        PluginHealthChecker.HealthResult a = PluginHealthChecker.HealthResult.ok(PluginState.STARTED);
        PluginHealthChecker.HealthResult b = PluginHealthChecker.HealthResult.ok(PluginState.STARTED);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void of_returnsEmptyList() {
        assertThat(PluginHealthChecker.of()).isEmpty();
    }

    @Test
    void check_implementation_example() {
        PluginHealthChecker checker = plugin -> {
            if (plugin.getState() == PluginState.STARTED) {
                return PluginHealthChecker.HealthResult.ok(plugin.getState());
            }
            return PluginHealthChecker.HealthResult.fail(plugin.getState(), "非运行状态");
        };
        PluginHealthChecker.HealthResult r = checker.check(new StubPlugin(PluginState.STARTED));
        assertThat(r.healthy()).isTrue();
    }

    record StubPlugin(PluginState state) implements Plugin {
        public cn.jowen.framework.plugin.descriptor.PluginDescriptor getDescriptor() {
            return new cn.jowen.framework.plugin.descriptor.PluginDescriptor("stub");
        }
        public PluginState getState() { return state; }
        public void start(PluginContext ctx) {}
        public void stop() {}
    }
}
