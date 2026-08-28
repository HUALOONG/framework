package cn.jowen.framework.extras.web.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SpelUtils} 测试。
 *
 * <p>覆盖：空表达式回退、非 {@code #} 前缀原样返回、按参数名/索引取值、嵌套属性、
 * 方法调用、字符串拼接、求值为 null 回退、表达式语法错误或引用不存在变量时回退。
 */
class SpelUtilsTest {

    /** 被解析的目标方法样例。 */
    @SuppressWarnings("unused")
    public static class Sample {

        public String describe(String userId, Order order, int count) {
            return userId;
        }

        public void noArgs() {
        }

        public String tag() {
            return "sample-tag";
        }
    }

    /** 嵌套对象样例。 */
    record Order(String id, Buyer buyer) {
    }

    /** 二级嵌套对象样例。 */
    record Buyer(String name) {
    }

    private static Method describeMethod() {
        try {
            return Sample.class.getDeclaredMethod("describe", String.class, Order.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static Method noArgsMethod() {
        try {
            return Sample.class.getDeclaredMethod("noArgs");
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static Object[] sampleArgs() {
        return new Object[]{"u-1", new Order("o-9", new Buyer("张三")), 3};
    }

    private static String resolve(String expression) {
        return SpelUtils.resolve(expression, new Sample(), describeMethod(), sampleArgs(), "fallback");
    }

    // ---------- 无需解析的分支 ----------

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "   "})
    void resolve_blankExpressionReturnsFallback(String expression) {
        assertThat(resolve(expression)).isEqualTo("fallback");
    }

    @Test
    void resolve_nullExpressionReturnsFallback() {
        assertThat(SpelUtils.resolve(null, new Sample(), describeMethod(), sampleArgs(), "fallback"))
                .isEqualTo("fallback");
    }

    @ParameterizedTest
    @ValueSource(strings = {"literal-key", "order:create", "a#b", "T(java.lang.String)"})
    void resolve_nonHashPrefixedExpressionIsReturnedVerbatim(String expression) {
        assertThat(resolve(expression)).isEqualTo(expression);
    }

    // ---------- 参数取值 ----------

    @Test
    void resolve_readsArgumentByParameterName() {
        assertThat(resolve("#userId")).isEqualTo("u-1");
    }

    @Test
    void resolve_readsArgumentByPositionalAlias() {
        assertThat(resolve("#p0")).isEqualTo("u-1");
        assertThat(resolve("#a0")).isEqualTo("u-1");
        assertThat(resolve("#p2")).isEqualTo("3");
    }

    @Test
    void resolve_readsNestedProperty() {
        assertThat(resolve("#order.id")).isEqualTo("o-9");
        assertThat(resolve("#order.buyer.name")).isEqualTo("张三");
    }

    @Test
    void resolve_supportsStringConcatenation() {
        assertThat(resolve("#userId + ':' + #order.id")).isEqualTo("u-1:o-9");
    }

    @Test
    void resolve_supportsArithmeticAndConvertsToString() {
        assertThat(resolve("#count * 2")).isEqualTo("6");
    }

    @Test
    void resolve_rootIsTheTargetObject() {
        // MethodBasedEvaluationContext 的 root / this 即传入的 target，可直接调用其公开方法
        assertThat(resolve("#root.tag()")).isEqualTo("sample-tag");
        assertThat(resolve("#this.tag()")).isEqualTo("sample-tag");
    }

    // ---------- 回退分支 ----------

    @Test
    void resolve_nullEvaluationResultReturnsFallback() {
        Object[] args = new Object[]{null, new Order("o-9", new Buyer("张三")), 3};

        assertThat(SpelUtils.resolve("#userId", new Sample(), describeMethod(), args, "fallback"))
                .isEqualTo("fallback");
    }

    @Test
    void resolve_unknownVariableReturnsFallback() {
        assertThat(resolve("#doesNotExist")).isEqualTo("fallback");
    }

    @Test
    void resolve_malformedExpressionReturnsFallback() {
        assertThat(resolve("#userId +")).isEqualTo("fallback");
        assertThat(resolve("#order.")).isEqualTo("fallback");
    }

    @Test
    void resolve_missingPropertyReturnsFallback() {
        assertThat(resolve("#order.nonExistingProperty")).isEqualTo("fallback");
    }

    @Test
    void resolve_expressionThrowingIsSwallowedIntoFallback() {
        Object[] args = new Object[]{"u-1", null, 3};

        assertThat(SpelUtils.resolve("#order.id", new Sample(), describeMethod(), args, "fallback"))
                .isEqualTo("fallback");
    }

    // ---------- 边界 ----------

    @Test
    void resolve_worksWithNoArgsMethod() {
        assertThat(SpelUtils.resolve("#p0", new Sample(), noArgsMethod(), new Object[0], "fallback"))
                .as("无参方法下引用参数应回退")
                .isEqualTo("fallback");
        assertThat(SpelUtils.resolve("#root.tag()", new Sample(), noArgsMethod(),
                new Object[0], "fallback")).isEqualTo("sample-tag");
    }

    @Test
    void resolve_acceptsNullTarget() {
        assertThat(SpelUtils.resolve("#userId", null, describeMethod(), sampleArgs(), "fallback"))
                .isEqualTo("u-1");
    }

    @Test
    void resolve_returnsFallbackNotNullWhenEverythingFails() {
        assertThat(SpelUtils.resolve("#bad(((", null, describeMethod(), sampleArgs(), "default-key"))
                .isEqualTo("default-key");
    }
}
