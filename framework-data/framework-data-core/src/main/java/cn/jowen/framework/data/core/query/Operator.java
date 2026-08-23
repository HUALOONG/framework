package cn.jowen.framework.data.core.query;

import org.jspecify.annotations.NullMarked;

/**
 * 查询操作符。由实现层翻译为 SQL 片段。
 */
@NullMarked
public enum Operator {

    /** 等于 */
    EQ,
    /** 不等于 */
    NE,
    /** 模糊匹配 */
    LIKE,
    /** 左模糊 */
    LIKE_LEFT,
    /** 右模糊 */
    LIKE_RIGHT,
    /** 大于 */
    GT,
    /** 小于 */
    LT,
    /** 大于等于 */
    GTE,
    /** 小于等于 */
    LTE,
    /** 区间内 */
    IN,
    /** 区间外 */
    NOT_IN,
    /** 闭区间 */
    BETWEEN,
    /** 为空 */
    IS_NULL,
    /** 不为空 */
    IS_NOT_NULL,
    /** 否定子条件 */
    NOT,
    /** 逻辑与 */
    AND,
    /** 逻辑或 */
    OR
}