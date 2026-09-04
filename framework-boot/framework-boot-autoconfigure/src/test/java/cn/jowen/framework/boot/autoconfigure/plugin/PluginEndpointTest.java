package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PluginEndpoint} 测试：列表查询与启动/停止/重启/卸载均委托给 {@link PluginManager}。
 */
class PluginEndpointTest {

    @Test
    void listPlugins_returnsStatusForEach() {
        Plugin p1 = mock(Plugin.class);
        when(p1.getDescriptor()).thenReturn(descriptor("a"));
        when(p1.getState()).thenReturn(PluginState.STARTED);
        Plugin p2 = mock(Plugin.class);
        when(p2.getDescriptor()).thenReturn(descriptor("b"));
        when(p2.getState()).thenReturn(PluginState.STOPPED);

        PluginManager manager = mock(PluginManager.class);
        when(manager.getPlugins()).thenReturn(List.of(p1, p2));
        PluginEndpoint endpoint = new PluginEndpoint(manager);

        List<PluginEndpoint.PluginStatus> statuses = endpoint.listPlugins();
        assertThat(statuses).hasSize(2);
        assertThat(statuses.get(0).pluginId()).isEqualTo("a");
        assertThat(statuses.get(0).state()).isEqualTo(PluginState.STARTED);
        assertThat(statuses.get(1).pluginId()).isEqualTo("b");
        assertThat(statuses.get(1).state()).isEqualTo(PluginState.STOPPED);
    }

    @Test
    void start_stop_restart_delegateToManager() {
        Plugin p = mock(Plugin.class);
        when(p.getState()).thenReturn(PluginState.STARTED);
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPlugin("x")).thenReturn(p);
        PluginEndpoint endpoint = new PluginEndpoint(manager);

        PluginEndpoint.PluginStatus started = endpoint.start("x");
        verify(manager).startPlugin("x");
        assertThat(started.pluginId()).isEqualTo("x");
        assertThat(started.state()).isEqualTo(PluginState.STARTED);

        endpoint.stop("x");
        verify(manager).stopPlugin("x");

        endpoint.restart("x");
        verify(manager).restartPlugin("x");
    }

    @Test
    void unload_delegatesToManager() {
        PluginManager manager = mock(PluginManager.class);
        PluginEndpoint endpoint = new PluginEndpoint(manager);

        endpoint.unload("x");
        verify(manager).unloadPlugin("x");
    }

    @Test
    void missingPlugin_statusHasNullState() {
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPlugin("ghost")).thenReturn(null);
        PluginEndpoint endpoint = new PluginEndpoint(manager);

        PluginEndpoint.PluginStatus status = endpoint.start("ghost");
        assertThat(status.pluginId()).isEqualTo("ghost");
        assertThat(status.state()).isNull();
    }

    private PluginDescriptor descriptor(String id) {
        return PluginDescriptor.of(id, "1.0", "c." + id);
    }
}
