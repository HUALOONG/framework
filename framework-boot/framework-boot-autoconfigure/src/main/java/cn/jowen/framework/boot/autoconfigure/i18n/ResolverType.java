package cn.jowen.framework.boot.autoconfigure.i18n;

import org.jspecify.annotations.NullMarked;

/**
 * 区域解析策略类型。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public enum ResolverType {

    /** 固定区域（单一语言）。 */
    FIXED,

    /** 请求参数（{@code ?lang=zh-CN}）。 */
    PARAMETER,

    /** Accept-Language 头。 */
    ACCEPT_HEADER,

    /** Cookie。 */
    COOKIE,

    /** 会话。 */
    SESSION,

    /** 组合（参数 → Cookie → Accept-Language）。 */
    COMPOSITE
}
