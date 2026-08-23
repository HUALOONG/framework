package cn.jowen.framework.plugin.support;

import org.jspecify.annotations.NullMarked;

/**
 * 校验错误。
 *
 * @param field    出错字段名
 * @param message  错误描述
 * @param severity 严重级别
 * @author 王飞
 */
@NullMarked
public record ValidationError(
        String field,
        String message,
        Severity severity
) {

    /**
     * 简化构造器，severity 默认 ERROR。
     */
    public ValidationError(String field, String message) {
        this(field, message, Severity.ERROR);
    }

    /**
     * 严重级别。
     */
    public enum Severity {
        WARNING,
        ERROR
    }
}
