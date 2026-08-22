package cn.jowen.framework.boot.autoconfigure;

import cn.jowen.framework.boot.autoconfigure.cache.CacheAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.data.DataJdbcAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.extras.ExtrasAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.health.HealthAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.i18n.I18nAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.logger.LoggerAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.observability.ObservabilityAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.plugin.PluginAutoConfiguration;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * 框架总装配入口。默认启用（{@code framework.enabled} 总开关，缺省即开），聚合各功能装配类。
 *
 * <p>当前引入 Logger 装配（M1）、JDBC 数据装配（M2）、缓存与国际化装配（M4）、插件装配（M5）、
 * extras/health/observability 装配（M6）；后续 web/bridge 逐步补齐。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework", name = "enabled", matchIfMissing = true)
@Import({
        LoggerAutoConfiguration.class,
        DataJdbcAutoConfiguration.class,
        CacheAutoConfiguration.class,
        I18nAutoConfiguration.class,
        PluginAutoConfiguration.class,
        ExtrasAutoConfiguration.class,
        HealthAutoConfiguration.class,
        ObservabilityAutoConfiguration.class
})
public class BootAutoConfiguration {
}
