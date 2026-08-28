package cn.jowen.framework.cache.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ConditionEvaluator} 单元测试。
 *
 * <p>{@link ConditionEvaluator} 为接口，唯一可执行代码是其默认方法 {@code evaluateUnless}。
 * 通过手写一个恒真/恒假的测试替身实现，验证默认方法对 {@code evaluate} 的取反语义
 * （true→不应缓存、false→应缓存）。无需 Mockito。
 */
class ConditionEvaluatorTest {

    /** 返回固定结果的测试替身，便于覆盖默认方法 {@code evaluateUnless} 的两个分支。 */
    private static final class ConstantEvaluator implements ConditionEvaluator {
        private final boolean value;

        ConstantEvaluator(boolean value) {
            this.value = value;
        }

        @Override
        public boolean evaluate(String condition, CacheOperationContext context) {
            return value;
        }
    }

    private static CacheOperationContext emptyContext() {
        return new CacheOperationContext(
                "bean", null, new Object[0], new Object(), "", "", "", "", false, "", "");
    }

    @Test
    void evaluateUnless_trueWhenUnderlyingFalse() {
        ConditionEvaluator evaluator = new ConstantEvaluator(false);
        assertThat(evaluator.evaluateUnless("cond", emptyContext())).isTrue();
    }

    @Test
    void evaluateUnless_falseWhenUnderlyingTrue() {
        ConditionEvaluator evaluator = new ConstantEvaluator(true);
        assertThat(evaluator.evaluateUnless("cond", emptyContext())).isFalse();
    }
}
