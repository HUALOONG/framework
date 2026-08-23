package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JDBC 装配属性：{@code framework.data.jdbc.*}。
 *
 * <p>继承 {@link JdbcProperties}，将 Spring 配置绑定到零 Spring 依赖的 JDBC 属性对象，
 * 供 {@link cn.jowen.framework.data.jdbc.context.JdbcContext} 装配使用。
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.data.jdbc")
public class BootJdbcProperties extends JdbcProperties {
}
