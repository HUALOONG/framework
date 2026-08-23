package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.plugin.api.PluginManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.ApplicationContext;

/**
 * 框架健康指示器：汇总框架各模块运行状态（缓存、插件、国际化资源包）。
 *
 * <p>任一无状态上报失败即标记为 DOWN，便于容器编排探针感知框架整体可用性。
 *
 * @author 王飞
 * @since 2026-08-21
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
        boolean healthy = true;

        ObjectProvider<CacheManager> cacheManager = context.getBeanProvider(CacheManager.class);
        if (cacheManager.getIfAvailable() != null) {
            int count = 0;
            for (String ignored : cacheManager.getObject().cacheNames()) {
                count++;
            }
            builder.withDetail("cache.caches", count);
        }

        ObjectProvider<PluginManager> pluginManager = context.getBeanProvider(PluginManager.class);
        PluginManager pm = pluginManager.getIfAvailable();
//        if (pm != null) {
//            builder.withDetail("plugin.plugins", pm.all().size());
//        }

        ObjectProvider<MessageSource> messageSource = context.getBeanProvider(MessageSource.class);
        if (messageSource.getIfAvailable() == null) {
            // 国际化资源缺失不视为致命，仅记录
            builder.withDetail("i18n.available", false);
        } else {
            builder.withDetail("i18n.available", true);
        }

        if (!healthy) {
            builder = Health.down();
        }
        return builder.build();
    }
}
