package cn.jowen.framework.data.core.sort;

/**
 * 排序中 NULL 值的处理策略。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
public enum NullHandling {

    /**
     * NULL 值排在前面。
     */
    NULLS_FIRST,

    /**
     * NULL 值排在后面。
     */
    NULLS_LAST
}
