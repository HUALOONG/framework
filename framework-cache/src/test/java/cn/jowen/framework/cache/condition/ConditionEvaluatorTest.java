package cn.jowen.framework.cache.condition;

import cn.jowen.framework.cache.support.CacheOperationContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ConditionEvaluator} 抽象层单元测试。
 *
 * <p>用“按表达式字面值求值”的替身实现（零 Spring 依赖），验证与求值引擎无关的契约：
 * 空/空白表达式的默认语义、{@code evaluateUnless} 的取反逻辑。
 */
class ConditionEvaluatorTest {

    @Test
    void blankConditionIsAlwaysTrue() {
        LiteralConditionEvaluator evaluator = new LiteralConditionEvaluator();
        CacheOperationContext ctx = emptyContext();
        assertThat(evaluator.evaluate("", ctx)).isTrue();
        assertThat(evaluator.evaluate("   ", ctx)).isTrue();
        assertThat(evaluator.evaluate(null, ctx)).isTrue();
    }

    @Test
    void blankUnlessIsAlwaysFalse() {
        LiteralConditionEvaluator evaluator = new LiteralConditionEvaluator();
        CacheOperationContext ctx = emptyContext();
        assertThat(evaluator.evaluateUnless("", ctx)).isFalse();
        assertThat(evaluator.evaluateUnless("   ", ctx)).isFalse();
        assertThat(evaluator.evaluateUnless(null, ctx)).isFalse();
    }

    @Test
    void evaluateUnlessReturnsExpressionValue() {
        // 语义与 Spring 一致：unless 表达式为真→不缓存；由调用方取反
        LiteralConditionEvaluator evaluator = new LiteralConditionEvaluator();
        CacheOperationContext ctx = emptyContext();
        assertThat(evaluator.evaluateUnless("value", ctx)).isTrue();
        assertThat(evaluator.evaluateUnless("other", ctx)).isFalse();
    }

    @Test
    void doEvaluateReceivesContext() {
        CapturingEvaluator evaluator = new CapturingEvaluator();
        CacheOperationContext ctx = emptyContext();
        evaluator.evaluate("any", ctx);
        assertThat(evaluator.capturedContext).isSameAs(ctx);
        assertThat(evaluator.capturedExpression).isEqualTo("any");
    }

    private static CacheOperationContext emptyContext() {
        return new CacheOperationContext("bean", null, new Object[0], new Object(),
                "cache", "key", "", "", false, "", "");
    }

    /** 表达式字面值等于“value”则为真，其余假。 */
    @NullMarked
    static class LiteralConditionEvaluator extends ConditionEvaluator {

        @Override
        protected boolean doEvaluate(String expression, CacheOperationContext context) {
            return "value".equals(expression);
        }
    }

    /** 捕获传入 doEvaluate 的表达式与上下文。 */
    static class CapturingEvaluator extends ConditionEvaluator {

        CacheOperationContext capturedContext;
        String capturedExpression;

        @Override
        protected boolean doEvaluate(String expression, CacheOperationContext context) {
            capturedContext = context;
            capturedExpression = expression;
            return true;
        }
    }
}