package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * 插件健康指示器：任一插件处于 FAILED 状态时上报 DOWN。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public class PluginHealthIndicator implements HealthIndicator {

    private final PluginManager pluginManager;

    /**
     * 构造健康指示器。
     *
     * @param pluginManager 插件管理器，不可为 {@code null}
     */
    public PluginHealthIndicator(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public Health health() {
        int total = 0;
        int failed = 0;
        for (Plugin plugin : pluginManager.getPlugins()) {
            total++;
            if (plugin.getState() == PluginState.FAILED) {
                failed++;
            }
        }
        Health.Builder builder = Health.up();
        builder.withDetail("plugins.total", total);
        builder.withDetail("plugins.failed", failed);
        if (failed > 0) {
            builder = Health.down();
        }
        return builder.build();
    }
}