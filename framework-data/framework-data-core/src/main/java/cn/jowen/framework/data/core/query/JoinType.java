package cn.jowen.framework.data.core.query;

import org.jspecify.annotations.NullMarked;

/**
 * 多表连接的连接类型。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public enum JoinType {

    /** 内连接 */
    INNER,

    /** 左外连接 */
    LEFT,

    /** 右外连接 */
    RIGHT,

    /** 全外连接 */
    FULL,

    /** 交叉连接 */
    CROSS
}