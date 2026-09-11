package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.condition.ConditionEvaluator;
import cn.jowen.framework.cache.support.CacheOperationContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 基于 Spring SpEL 的 {@link ConditionEvaluator} 实现。
 *
 * <p>评估 {@link cn.jowen.framework.cache.annotation.Cacheable} 与
 * {@link cn.jowen.framework.cache.annotation.CachePut} 的 {@code condition} /
 * {@code unless} 表达式，支持按参数名（如 {@code #id}）与按索引（{@code #a0} / {@code #p0}）
 * 引用方法参数。
 *
 * <p>空/空白表达式约定为“条件恒成立”，与 Spring 的处理语义一致。
 *
 * <p>本类位于 {@code framework-boot-autoconfigure} 而非 {@code framework-cache}：
 * 后者是零 Spring 依赖的核心抽象层，SpEL 求值属于 Spring 生态的适配层。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
public class SpelConditionEvaluator extends ConditionEvaluator {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    @Override
    protected boolean doEvaluate(String expression, CacheOperationContext context) {
        EvaluationContext ctx = buildContext(context);
        Object value = PARSER.parseExpression(expression).getValue(ctx);
        return toBoolean(value);
    }

    /**
     * 构造 SpEL 求值上下文，注入索引变量（#a0/#p0）与参数名变量（#id）。
     * 由 {@link SpringCacheAnnotationProcessor#evaluateSpEL} 与 {@link #doEvaluate} 共用。
     */
    static EvaluationContext buildContext(CacheOperationContext context) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("target", context.target());
        Object[] args = context.args() == null ? new Object[0] : context.args();
        for (int i = 0; i < args.length; i++) {
            ctx.setVariable("a" + i, args[i]);
            ctx.setVariable("p" + i, args[i]);
        }
        // 按参数名引用（如 #id）需要编译期保留参数名（-parameters）
        String[] names = context.method() == null ? null : PARAMETER_NAMES.getParameterNames(context.method());
        for (int i = 0; i < args.length; i++) {
            if (names != null && i < names.length && names[i] != null) {
                ctx.setVariable(names[i], args[i]);
            }
        }
        return ctx;
    }

    private static boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String s = value.toString();
        return !s.isEmpty() && !"false".equalsIgnoreCase(s) && !"null".equalsIgnoreCase(s);
    }
}