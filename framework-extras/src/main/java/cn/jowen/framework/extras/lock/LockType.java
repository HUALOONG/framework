package cn.jowen.framework.extras.lock;

import org.jspecify.annotations.NullMarked;

/**
 * 分布式锁类型枚举。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public enum LockType {
    /** 可重入锁（默认） */
    REENTRANT,
    /** 公平锁 */
    FAIR,
    /** 读锁 */
    READ,
    /** 写锁 */
    WRITE,
    /** 联锁（多个锁同时持有） */
    MULTI,
    /** 红锁（多个 Redis 节点） */
    RED
}
