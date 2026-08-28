package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;

/**
 * 锁类型枚举。
 *
 * <ul>
 *   <li>{@link #LOCAL}：基于 JVM 本地锁（单实例适用）。</li>
 *   <li>{@link #REDIS}：基于 Redis 的分布式锁（多实例适用，需自行提供 {@link DistributedLock} 实现）。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum LockType {

    /** 本地锁 */
    LOCAL,

    /** 分布式锁（Redis 等） */
    REDIS
}
