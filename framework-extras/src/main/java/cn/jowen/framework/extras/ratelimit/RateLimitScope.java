package cn.jowen.framework.extras.ratelimit;

import org.jspecify.annotations.NullMarked;

/**
 * 限流作用域枚举。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public enum RateLimitScope {
    /** 全局 */
    GLOBAL,
    /** 用户维度 */
    USER,
    /** IP 维度 */
    IP,
    /** 自定义维度 */
    CUSTOM
}
