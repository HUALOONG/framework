package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SmsCaptchaGenerator} 测试。
 *
 * <p>覆盖：类型标识、验证码位数与纯数字（含前导零补齐）、发送回调参数、
 * 手机号为空的校验、无参 generate 的不支持语义、发送器异常向外传播。
 */
class SmsCaptchaGeneratorTest {

    /** 记录发送内容的测试替身，不发起任何真实外部调用。 */
    private static final class RecordingSender implements SmsCaptchaSender {
        private final List<String> phones = new ArrayList<>();
        private final List<String> codes = new ArrayList<>();

        @Override
        public void send(String phone, String code) {
            phones.add(phone);
            codes.add(code);
        }
    }

    @Test
    void type_isSms() {
        assertThat(new SmsCaptchaGenerator(6, new RecordingSender()).type())
                .isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.SMS);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 6, 8})
    void generate_codeHasConfiguredDigitLength(int length) {
        RecordingSender sender = new RecordingSender();
        SmsCaptchaGenerator generator = new SmsCaptchaGenerator(length, sender);

        for (int i = 0; i < 30; i++) {
            Captcha captcha = generator.generate("13800000000", 60);
            assertThat(captcha.code()).hasSize(length).containsOnlyDigits();
        }
    }

    @Test
    void generate_zeroLengthFallsBackToSingleDigit() {
        Captcha captcha = new SmsCaptchaGenerator(0, new RecordingSender()).generate("138", 60);

        assertThat(captcha.code()).hasSize(1).containsOnlyDigits();
    }

    @Test
    void generate_passesPhoneAndCodeToSender() {
        RecordingSender sender = new RecordingSender();
        Captcha captcha = new SmsCaptchaGenerator(6, sender).generate("13900001111", 60);

        assertThat(sender.phones).containsExactly("13900001111");
        assertThat(sender.codes).containsExactly(captcha.code());
    }

    @Test
    void generate_sendsExactlyOncePerInvocation() {
        AtomicInteger count = new AtomicInteger();
        SmsCaptchaGenerator generator = new SmsCaptchaGenerator(4, (phone, code) -> count.incrementAndGet());

        generator.generate("138", 60);
        generator.generate("138", 60);

        assertThat(count.get()).isEqualTo(2);
    }

    @Test
    void generate_hasNoImageAndIdIsUnique() {
        SmsCaptchaGenerator generator = new SmsCaptchaGenerator(6, new RecordingSender());
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            Captcha captcha = generator.generate("138", 60);
            assertThat(captcha.image()).isNull();
            ids.add(captcha.id());
        }

        assertThat(ids).hasSize(50);
    }

    /** 现状记录：实现将未使用的空 StringBuilder 作为 text，故 text 为空串而非 null。 */
    @Test
    void generate_textIsEmptyString() {
        assertThat(new SmsCaptchaGenerator(6, new RecordingSender()).generate("138", 60).text())
                .isEmpty();
    }

    @Test
    void generate_expireAtReflectsExpireSeconds() {
        long before = System.currentTimeMillis();
        Captcha captcha = new SmsCaptchaGenerator(6, new RecordingSender()).generate("138", 90);
        long after = System.currentTimeMillis();

        assertThat(captcha.expireAt()).isBetween(before + 90_000L, after + 90_000L);
    }

    // ---------- 校验与失败路径 ----------

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void generate_rejectsBlankPhone(String phone) {
        RecordingSender sender = new RecordingSender();

        assertThatThrownBy(() -> new SmsCaptchaGenerator(6, sender).generate(phone, 60))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("手机号不能为空");
        assertThat(sender.phones).as("校验失败不应触发发送").isEmpty();
    }

    @Test
    void generate_rejectsNullPhone() {
        RecordingSender sender = new RecordingSender();

        assertThatThrownBy(() -> new SmsCaptchaGenerator(6, sender).generate(null, 60))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("手机号不能为空");
        assertThat(sender.phones).isEmpty();
    }

    @Test
    void generateWithoutPhone_isUnsupported() {
        SmsCaptchaGenerator generator = new SmsCaptchaGenerator(6, new RecordingSender());

        assertThatThrownBy(() -> generator.generate(60L))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("短信验证码需指定接收手机号");
    }

    @Test
    void generate_propagatesSenderFailure() {
        SmsCaptchaGenerator generator = new SmsCaptchaGenerator(6, (phone, code) -> {
            throw new IllegalStateException("短信网关不可用");
        });

        assertThatThrownBy(() -> generator.generate("138", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("短信网关不可用");
    }

    @Test
    void implementsCaptchaGeneratorContract() {
        assertThat(new SmsCaptchaGenerator(6, new RecordingSender()))
                .isInstanceOf(CaptchaGenerator.class);
    }
}
