package cn.jowen.framework.data.core.transaction;

import org.jspecify.annotations.NullMarked;

/**
 * 事务隔离级别。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum Isolation {
    DEFAULT,
    READ_UNCOMMITTED,
    READ_COMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE
}