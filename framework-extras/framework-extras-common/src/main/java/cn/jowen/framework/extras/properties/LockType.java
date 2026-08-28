package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 锁类型枚举。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum LockType {

    /** 可重入锁 */
    REENTRANT,

    /** 公平锁 */
    FAIR,

    /** 读锁（共享，与写锁互斥） */
    READ,

    /** 写锁（独占，与读锁、写锁均互斥） */
    WRITE,

    /** 联锁：需同时获取多把锁才算成功，任一失败则整体回滚 */
    MULTI,

    /** 红锁：在多数独立节点上获取成功才算成功 */
    RED
}
