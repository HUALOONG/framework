package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootHealthIndicatorTest {

    @Mock
    ApplicationContext ctx;

    @Test
    void noOptionalBeans_isUp() {
        stubProviders(null, null, null);
        BootHealthIndicator indicator = new BootHealthIndicator(ctx);
        assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void cacheManager_countsCaches() {
        CacheManager cm = mock(CacheManager.class);
        when(cm.cacheNames()).thenReturn(Set.of("a", "b"));
        stubProviders(cm, null, null);
        BootHealthIndicator indicator = new BootHealthIndicator(ctx);
        assertThat(indicator.health().getDetails().get("cache.caches")).isEqualTo(2);
    }

    @Test
    void cacheManager_error_marksDown() {
        CacheManager cm = mock(CacheManager.class);
        when(cm.cacheNames()).thenThrow(new RuntimeException("boom"));
        stubProviders(cm, null, null);
        BootHealthIndicator indicator = new BootHealthIndicator(ctx);
        assertThat(indicator.health().getStatus().getCode()).isEqualTo("DOWN");
    }

    @Test
    void pluginManager_failedPlugins_marksDown() {
        PluginManager pm = mock(PluginManager.class);
        Plugin failed = mock(Plugin.class);
        when(pm.getPlugins()).thenReturn(List.of(failed));
        when(pm.getPluginsByState(PluginState.FAILED)).thenReturn(List.of(failed));
        stubProviders(null, pm, null);
        BootHealthIndicator indicator = new BootHealthIndicator(ctx);
        var health = indicator.health();
        assertThat(health.getDetails().get("plugin.plugins")).isEqualTo(1);
        assertThat(health.getDetails().get("plugin.failed")).isEqualTo(1);
        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
    }

    @Test
    void pluginManager_noFailures_isUp() {
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugins()).thenReturn(List.of(mock(Plugin.class)));
        when(pm.getPluginsByState(PluginState.FAILED)).thenReturn(List.of());
        stubProviders(null, pm, null);
        BootHealthIndicator indicator = new BootHealthIndicator(ctx);
        assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void i18nAvailable_recorded() {
        MessageSource ms = mock(MessageSource.class);
        stubProviders(null, null, ms);
        BootHealthIndicator indicator = new BootHealthIndicator(ctx);
        assertThat(indicator.health().getDetails().get("i18n.available")).isEqualTo(true);
    }

    private void stubProviders(CacheManager cm, PluginManager pm, MessageSource ms) {
        ObjectProvider<CacheManager> cmProv = mock(ObjectProvider.class);
        when(cmProv.getIfAvailable()).thenReturn(cm);
        ObjectProvider<PluginManager> pmProv = mock(ObjectProvider.class);
        when(pmProv.getIfAvailable()).thenReturn(pm);
        ObjectProvider<MessageSource> msProv = mock(ObjectProvider.class);
        when(msProv.getIfAvailable()).thenReturn(ms);
        when(ctx.getBeanProvider(CacheManager.class)).thenReturn(cmProv);
        when(ctx.getBeanProvider(PluginManager.class)).thenReturn(pmProv);
        when(ctx.getBeanProvider(MessageSource.class)).thenReturn(msProv);
    }
}
