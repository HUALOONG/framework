package cn.jowen.framework.boot.autoconfigure.data;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据源相关配置属性载体。实际 {@code DataSource} 仍由 Spring Boot 原生 {@code spring-boot-starter-jdbc}
 * 自动装配管理（基于 {@code spring.datasource.*}）；此处仅承载框架自身的开关与方言选择，避免重复造轮子。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.data")
public class DataSourceProperties {

    /** 是否启用框架 JDBC 仓储装配，缺省开启。 */
    private boolean enabled = true;

    /** SQL 方言。可选 {@code standard}（H2/MySQL/PostgreSQL 兼容），缺省 {@code standard}。 */
    private String dialect = "standard";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }
}
