package cn.jowen.framework.cache.support;

import org.jspecify.annotations.NullMarked;

/**
 * 条件表达式解析器接口。用于评估 @Cacheable/@CachePut 的 condition/unless SpEL 表达式。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface ConditionEvaluator {

    /**
     * 评估条件表达式。
     *
     * @param condition SpEL 条件表达式，不可为 {@code null}
     * @param context   缓存操作上下文，不可为 {@code null}
     * @return {@code true} 表示条件满足（应缓存），{@code false} 表示不满足
     */
    boolean evaluate(String condition, CacheOperationContext context);

    /**
     * 评估否定条件（unless）。
     *
     * @param unless  SpEL 否定条件表达式，不可为 {@code null}
     * @param context 缓存操作上下文，不可为 {@code null}
     * @return {@code true} 表示不应缓存，{@code false} 表示正常缓存
     */
    default boolean evaluateUnless(String unless, CacheOperationContext context) {
        return !evaluate(unless, context);
    }
}
