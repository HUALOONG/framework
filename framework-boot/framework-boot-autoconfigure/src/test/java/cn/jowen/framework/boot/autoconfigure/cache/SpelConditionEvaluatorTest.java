package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.support.CacheOperationContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SpelConditionEvaluator} 单元测试。
 *
 * <p>验证 SpEL 引擎下的具体求值语义：参数名/索引变量、类型转换、空结果等。
 */
class SpelConditionEvaluatorTest {

    private final SpelConditionEvaluator evaluator = new SpelConditionEvaluator();

    private CacheOperationContext emptyContext() {
        return new CacheOperationContext("bean", null, new Object[0], new Object(),
                "cache", "key", "", "", false, "", "");
    }

    private CacheOperationContext paramContext(String id, String name) throws NoSuchMethodException {
        Method m = Target.class.getMethod("getUser", String.class, String.class);
        return new CacheOperationContext("bean", m, new Object[]{id, name}, new Object(),
                "cache", "key", "", "", false, "", "");
    }

    @Test
    void blankConditionIsAlwaysTrue() {
        assertThat(evaluator.evaluate("", emptyContext())).isTrue();
        assertThat(evaluator.evaluate("   ", emptyContext())).isTrue();
        assertThat(evaluator.evaluate(null, emptyContext())).isTrue();
    }

    @Test
    void blankUnlessIsAlwaysFalse() {
        assertThat(evaluator.evaluateUnless("", emptyContext())).isFalse();
        assertThat(evaluator.evaluateUnless("   ", emptyContext())).isFalse();
        assertThat(evaluator.evaluateUnless(null, emptyContext())).isFalse();
    }

    @Test
    void numericExpressionResolvesToBoolean() {
        assertThat(evaluator.evaluate("1", emptyContext())).isTrue();
        assertThat(evaluator.evaluate("0", emptyContext())).isFalse();
    }

    @Test
    void stringExpressionResolvesToBoolean() {
        assertThat(evaluator.evaluate("'yes'", emptyContext())).isTrue();
        assertThat(evaluator.evaluate("''", emptyContext())).isFalse();
    }

    @Test
    void nullResultIsFalse() {
        assertThat(evaluator.evaluate("null", emptyContext())).isFalse();
        assertThat(evaluator.evaluate("#noSuchVariable", emptyContext())).isFalse();
    }

    @Test
    void indexVariableWorks() throws NoSuchMethodException {
        CacheOperationContext ctx = paramContext("u-1", "User");
        assertThat(evaluator.evaluate("#a0 == 'u-1'", ctx)).isTrue();
        assertThat(evaluator.evaluate("#p1 == 'User'", ctx)).isTrue();
    }

    @Test
    void parameterNameVariableWorks() throws NoSuchMethodException {
        CacheOperationContext ctx = paramContext("u-1", "User");
        assertThat(evaluator.evaluate("#id == 'u-1'", ctx)).isTrue();
        assertThat(evaluator.evaluate("#name == 'User'", ctx)).isTrue();
    }

    @Test
    void targetVariableAvailable() throws NoSuchMethodException {
        CacheOperationContext ctx = paramContext("u-1", "User");
        assertThat(evaluator.evaluate("#target != null", ctx)).isTrue();
    }

    @Test
    void comparisonExpressionWorks() throws NoSuchMethodException {
        CacheOperationContext ctx = paramContext("u-1", "User");
        assertThat(evaluator.evaluate("#id.length() > 2", ctx)).isTrue();
        assertThat(evaluator.evaluate("#name.contains('ser')", ctx)).isTrue();
    }

    @Test
    void unlessReturnsExpressionValue() {
        assertThat(evaluator.evaluateUnless("true", emptyContext())).isTrue();
        assertThat(evaluator.evaluateUnless("false", emptyContext())).isFalse();
    }

    @Test
    void customParsingContextCanBeOverridden() throws NoSuchMethodException {
        CustomEvaluator evaluator = new CustomEvaluator();
        CacheOperationContext ctx = paramContext("u-1", "User");
        assertThat(evaluator.evaluate("any", ctx)).isTrue();
    }

    static class Target {
        public String getUser(String id, String name) {
            return id + "-" + name;
        }
    }

    static class CustomEvaluator extends SpelConditionEvaluator {
        @Override
        protected boolean doEvaluate(String expression, CacheOperationContext context) {
            return true;
        }
    }
}
