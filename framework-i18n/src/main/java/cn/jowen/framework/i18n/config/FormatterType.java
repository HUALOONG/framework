package cn.jowen.framework.i18n.config;

import org.jspecify.annotations.NullMarked;

/**
 * 消息格式化策略类型。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public enum FormatterType {

    /**
     * JDK MessageFormat（默认）。
     */
    JAVA_TEXT,

    /**
     * 命名参数（{@code {name}} + Map）。
     */
    NAMED_PARAMETER,

    /**
     * ICU4J（需引入 com.ibm.icu:icu4j）。
     */
    ICU
}
