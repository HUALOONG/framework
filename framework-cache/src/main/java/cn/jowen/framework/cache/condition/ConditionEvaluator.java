package cn.jowen.framework.cache.condition;

import cn.jowen.framework.cache.support.CacheOperationContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 条件表达式解析器抽象。
 *
 * <p>用于评估 {@link cn.jowen.framework.cache.annotation.Cacheable} 与
 * {@link cn.jowen.framework.cache.annotation.CachePut} 的 {@code condition}（满足条件才缓存）
 * 以及 {@code unless}（满足条件则不缓存）表达式。
 *
 * <p>约定：
 * <ul>
 * <li>空/空白表达式视为“恒成立”（同 Spring 语义）；</li>
 * <li>本抽象层零 Spring 依赖，仅定义契约；</li>
 * <li>子类只需实现 {@link #doEvaluate(String, CacheOperationContext)}，默认方法 {@code evaluateUnless}
 *     自动取反。</li>
 * </ul>
 *
 * <p>框架提供一个基于 Spring SpEL 的即用实现
 * {@link cn.jowen.framework.boot.autoconfigure.cache.SpelConditionEvaluator}，
 * 业务方也可自行扩展（例如基于 Aviator、QLExpress 等轻量引擎）。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
public abstract class ConditionEvaluator {

    /**
     * 评估 {@code condition} 表达式。
     *
     * @param condition 条件表达式，可为空或空白（视为恒成立）
     * @param context   缓存操作上下文
     * @return {@code true} 表示条件满足（应继续缓存操作），{@code false} 表示不满足（跳过）
     */
    public boolean evaluate(@Nullable String condition, CacheOperationContext context) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        return doEvaluate(condition, context);
    }

    /**
     * 评估 {@code unless} 否定条件。
     *
     * <p>语义与 <a href="https://docs.spring.io/spring-framework/reference/core/cache/annotation-enable.html">Spring</a>
     * 一致：表达式为真时<strong>不</strong>缓存。调用方需自行取反（{@code if (!evaluator.evaluateUnless(unless, ctx)) put()}）。
     *
     * @param unless  否定条件表达式，可为空或空白（视为恒不成立）
     * @param context 缓存操作上下文
     * @return {@code true} 表示不应缓存，{@code false} 表示正常缓存
     */
    public boolean evaluateUnless(@Nullable String unless, CacheOperationContext context) {
        if (unless == null || unless.isBlank()) {
            return false;
        }
        return doEvaluate(unless, context);
    }

    /**
     * 由子类实现具体的表达式求值。
     *
     * @param expression 已确认非空白的表达式
     * @param context    缓存操作上下文，子类从中解析变量（按名、按索引或自定义机制）
     * @return 表达式的布尔结果
     */
    protected abstract boolean doEvaluate(String expression, CacheOperationContext context);
}