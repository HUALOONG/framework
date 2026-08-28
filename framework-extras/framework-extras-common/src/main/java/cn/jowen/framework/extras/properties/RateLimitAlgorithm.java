package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 限流算法枚举。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum RateLimitAlgorithm {

    /** 固定窗口 */
    FIXED_WINDOW,

    /** 滑动窗口 */
    SLIDING_WINDOW,

    /** 漏桶 */
    LEAKY_BUCKET,

    /** 令牌桶 */
    TOKEN_BUCKET
}
