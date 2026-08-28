package cn.jowen.framework.data.core.datasource;

import org.jspecify.annotations.NullMarked;

/**
 * 连接池类型。由实现层根据枚举选择对应的连接池驱动。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum PoolType {

    /** HikariCP，默认连接池。 */
    HIKARI,

    /** Druid 连接池，含内置监控。 */
    DRUID,

    /** 无连接池，直接使用裸连接（测试场景）。 */
    NONE,

    /** 简易连接（基于 DriverManager 直连，无连接池），与 {@code NONE} 等价但语义更直观。 */
    SIMPLE
}