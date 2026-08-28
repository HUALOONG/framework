package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ArithmeticCaptchaGenerator} 测试。
 *
 * <p>覆盖：类型标识、题面格式、答案与题面算式一致、操作数取值范围随 length 变化、
 * id 唯一、过期时间按 expireSeconds 计算。
 */
class ArithmeticCaptchaGeneratorTest {

    @Test
    void type_isArithmetic() {
        assertThat(new ArithmeticCaptchaGenerator(4).type())
                .isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC);
    }

    @Test
    void generate_producesQuestionInExpectedFormat() {
        Captcha captcha = new ArithmeticCaptchaGenerator(4).generate(60);

        assertThat(captcha.text()).isNotNull().matches("\\d+ \\+ \\d+ = \\?");
    }

    @Test
    void generate_answerMatchesQuestion() {
        ArithmeticCaptchaGenerator generator = new ArithmeticCaptchaGenerator(4);

        for (int i = 0; i < 200; i++) {
            Captcha captcha = generator.generate(60);
            String question = captcha.text();
            assertThat(question).isNotNull();
            String[] operands = question.replace("= ?", "").split("\\+");
            int expected = Integer.parseInt(operands[0].trim()) + Integer.parseInt(operands[1].trim());
            assertThat(captcha.code()).isEqualTo(String.valueOf(expected));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 6})
    void generate_operandsStayWithinBoundDerivedFromLength(int length) {
        ArithmeticCaptchaGenerator generator = new ArithmeticCaptchaGenerator(length);
        int bound = Math.max(10, 10 * length);

        for (int i = 0; i < 100; i++) {
            String question = generator.generate(60).text();
            assertThat(question).isNotNull();
            String[] operands = question.replace("= ?", "").split("\\+");
            int a = Integer.parseInt(operands[0].trim());
            int b = Integer.parseInt(operands[1].trim());

            assertThat(a).isBetween(1, bound - 1);
            assertThat(b).isBetween(1, bound - 1);
        }
    }

    @Test
    void generate_hasNoImagePayload() {
        assertThat(new ArithmeticCaptchaGenerator(4).generate(60).image()).isNull();
    }

    @Test
    void generate_idIsUniquePerInvocation() {
        ArithmeticCaptchaGenerator generator = new ArithmeticCaptchaGenerator(4);
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            ids.add(generator.generate(60).id());
        }

        assertThat(ids).hasSize(200);
    }

    @Test
    void generate_expireAtReflectsExpireSeconds() {
        long before = System.currentTimeMillis();
        Captcha captcha = new ArithmeticCaptchaGenerator(4).generate(120);
        long after = System.currentTimeMillis();

        assertThat(captcha.expireAt()).isBetween(before + 120_000L, after + 120_000L);
        assertThat(captcha.expired()).isFalse();
    }

    @Test
    void generate_withZeroExpireIsImmediatelyExpired() throws Exception {
        Captcha captcha = new ArithmeticCaptchaGenerator(4).generate(0);
        Thread.sleep(5L);

        assertThat(captcha.expired()).isTrue();
    }

    @Test
    void implementsCaptchaGeneratorContract() {
        assertThat(new ArithmeticCaptchaGenerator(4)).isInstanceOf(CaptchaGenerator.class);
    }
}
