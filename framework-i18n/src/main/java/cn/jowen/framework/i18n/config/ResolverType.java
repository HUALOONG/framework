package cn.jowen.framework.i18n.config;

import org.jspecify.annotations.NullMarked;

/**
 * 区域解析策略类型。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum ResolverType {

    /**
     * 固定区域（单一语言）。
     */
    FIXED,

    /**
     * 请求参数（{@code ?lang=zh-CN}）。
     */
    PARAMETER,

    /**
     * Accept-Language 头。
     */
    ACCEPT_HEADER,

    /**
     * Cookie。
     */
    COOKIE,

    /**
     * 会话。
     */
    SESSION,

    /**
     * 组合（参数 → Cookie → Accept-Language）。
     */
    COMPOSITE
}
