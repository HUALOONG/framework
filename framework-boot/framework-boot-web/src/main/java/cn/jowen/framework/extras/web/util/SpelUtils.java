package cn.jowen.framework.extras.web.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/**
 * SpEL 表达式求值工具，供各 AOP 切面解析注解中的 {@code key} 表达式。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SpelUtils {

    /** PARSER 常量。 */
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    /** DISCOVERER 常量。 */
    private static final ParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();

    private SpelUtils() {
    }

    /**
     * 解析 SpEL 表达式。
     *
     * @param expression 表达式字符串（非空且以 {@code #} 开头才解析，否则原样返回）
     * @param target     目标对象
     * @param method     目标方法
     * @param args       方法实参
     * @return 求值结果字符串；表达式为空或无需解析时返回 {@code fallback}
     * @param fallback 参数 fallback
     */
    public static String resolve(String expression, @Nullable Object target, Method method,
                                 Object[] args, String fallback) {
        if (expression == null || expression.isBlank()) {
            return fallback;
        }
        if (!expression.startsWith("#")) {
            return expression;
        }
        try {
            Expression exp = PARSER.parseExpression(expression);
            MethodBasedEvaluationContext ctx =
                    new MethodBasedEvaluationContext(target, method, args, DISCOVERER);
            Object value = exp.getValue(ctx);
            return value == null ? fallback : value.toString();
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
