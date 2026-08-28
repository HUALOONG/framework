package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * 框架健康指示器：汇总框架各模块运行状态（缓存、插件、国际化资源包）。
 *
 * <p>任一无状态上报失败即标记为 DOWN，便于容器编排探针感知框架整体可用性。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class BootHealthIndicator implements HealthIndicator {

    private final ApplicationContext context;

    public BootHealthIndicator(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        ObjectProvider<CacheManager> cacheManager = context.getBeanProvider(CacheManager.class);
        CacheManager cm = cacheManager.getIfAvailable();
        if (cm != null) {
            try {
                int count = 0;
                for (String ignored : cm.cacheNames()) {
                    count++;
                }
                builder.withDetail("cache.caches", count);
            } catch (Exception e) {
                // 缓存管理器存在但无法访问：视为不可用
                builder.withDetail("cache.error", e.getMessage());
                builder.down();
            }
        }

        ObjectProvider<PluginManager> pluginManager = context.getBeanProvider(PluginManager.class);
        PluginManager pm = pluginManager.getIfAvailable();
        if (pm != null) {
            List<Plugin> plugins = pm.getPlugins();
            builder.withDetail("plugin.plugins", plugins.size());
            List<Plugin> failed = pm.getPluginsByState(PluginState.FAILED);
            builder.withDetail("plugin.failed", failed.size());
            if (!failed.isEmpty()) {
                builder.down();
            }
        }

        ObjectProvider<MessageSource> messageSource = context.getBeanProvider(MessageSource.class);
        // 国际化资源缺失不视为致命，仅记录
        builder.withDetail("i18n.available", messageSource.getIfAvailable() != null);

        return builder.build();
    }
}
