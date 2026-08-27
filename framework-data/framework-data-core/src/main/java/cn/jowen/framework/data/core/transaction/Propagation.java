package cn.jowen.framework.data.core.transaction;

import org.jspecify.annotations.NullMarked;

/**
 * 事务传播行为。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public enum Propagation {
    REQUIRED,
    REQUIRES_NEW,
    NESTED,
    SUPPORTS,
    NOT_SUPPORTED,
    NEVER,
    MANDATORY
}