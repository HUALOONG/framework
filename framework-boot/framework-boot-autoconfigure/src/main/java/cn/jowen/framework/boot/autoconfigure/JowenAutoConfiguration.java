package cn.jowen.framework.boot.autoconfigure;

import cn.jowen.framework.boot.autoconfigure.cache.CacheAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.data.DataSourceAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.data.JdbcAutoConfiguration;
import cn.jowen.framework.boot.autoconfigure.data.MybatisAutoConfiguration;
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
 * <p>当前引入 Logger 装配、数据装配（JDBC/MyBatis/数据源抽象）、缓存、国际化、可观测性、
 * 插件与 extras/health 装配。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework", name = "enabled", matchIfMissing = true)
@Import({
        CacheAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        ExtrasAutoConfiguration.class,
        HealthAutoConfiguration.class,
        I18nAutoConfiguration.class,
        JdbcAutoConfiguration.class,
        LoggerAutoConfiguration.class,
        MybatisAutoConfiguration.class,
        ObservabilityAutoConfiguration.class,
        PluginAutoConfiguration.class
})
public class JowenAutoConfiguration {
}
