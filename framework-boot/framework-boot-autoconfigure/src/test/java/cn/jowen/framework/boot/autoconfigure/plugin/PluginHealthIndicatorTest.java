package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginHealthIndicatorTest {

    @Mock
    private PluginManager pluginManager;

    @Test
    void health_up_whenNoPluginFailed() {
        Plugin p1 = mock(Plugin.class);
        when(p1.getState()).thenReturn(PluginState.STARTED);
        Plugin p2 = mock(Plugin.class);
        when(p2.getState()).thenReturn(PluginState.STOPPED);
        when(pluginManager.getPlugins()).thenReturn(List.of(p1, p2));

        Health health = new PluginHealthIndicator(pluginManager).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void health_down_whenAnyPluginFailed() {
        Plugin p1 = mock(Plugin.class);
        when(p1.getState()).thenReturn(PluginState.STARTED);
        Plugin p2 = mock(Plugin.class);
        when(p2.getState()).thenReturn(PluginState.FAILED);
        when(pluginManager.getPlugins()).thenReturn(List.of(p1, p2));

        Health health = new PluginHealthIndicator(pluginManager).health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
    }

    @Test
    void health_up_whenNoPlugins() {
        when(pluginManager.getPlugins()).thenReturn(List.of());

        Health health = new PluginHealthIndicator(pluginManager).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
    }
}
