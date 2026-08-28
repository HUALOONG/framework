package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CaptchaService} 生成与校验验证。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class CaptchaServiceTest {

    private static ExtrasWebProperties.Captcha propsOf(ExtrasWebProperties.Captcha.CaptchaType type) {
        ExtrasWebProperties.Captcha props = new ExtrasWebProperties.Captcha();
        props.setType(type);
        props.setExpireSeconds(60L);
        return props;
    }

    private static CaptchaService serviceOf(ExtrasWebProperties.Captcha.CaptchaType type) {
        return new CaptchaService(new CaptchaStore.InMemory(),
                List.of(new ArithmeticCaptchaGenerator(4), new GraphicCaptchaGenerator(80, 30, 4),
                        new SliderCaptchaGenerator(200, 100, 40)),
                propsOf(type));
    }

    @Test
    void arithmeticCaptchaIsGeneratedAndValidated() {
        CaptchaService service = serviceOf(ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC);

        Captcha captcha = service.generate();
        assertThat(captcha.text()).contains("+").contains("= ?");
        assertThat(captcha.code()).isNotBlank();

        // 从题面反解答案，验证校验逻辑
        String[] parts = captcha.text().replace("= ?", "").split("\\+");
        int expected = Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        assertThat(service.validate(captcha.id(), String.valueOf(expected))).isTrue();
    }

    @Test
    void validateIsOneTimeOnly() {
        CaptchaService service = serviceOf(ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC);
        Captcha captcha = service.generate();

        String[] parts = captcha.text().replace("= ?", "").split("\\+");
        int expected = Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());

        assertThat(service.validate(captcha.id(), String.valueOf(expected))).isTrue();
        // 二次校验应失败（已失效）
        assertThat(service.validate(captcha.id(), String.valueOf(expected))).isFalse();
    }

    @Test
    void validateRejectsWrongCode() {
        CaptchaService service = serviceOf(ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC);
        Captcha captcha = service.generate();

        assertThat(service.validate(captcha.id(), "definitely-wrong")).isFalse();
    }

    @Test
    void validateRejectsUnknownId() {
        assertThat(serviceOf(ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC)
                .validate("no-such-id", "1")).isFalse();
    }

    @Test
    void graphicCaptchaProducesPngImage() {
        Captcha captcha = serviceOf(ExtrasWebProperties.Captcha.CaptchaType.GRAPHIC).generate();
        assertThat(captcha.image()).isNotNull().isNotEmpty();
        // PNG 魔数
        assertThat(captcha.image()[0]).isEqualTo((byte) 0x89);
        assertThat(captcha.image()[1]).isEqualTo((byte) 'P');
    }

    @Test
    void sliderCaptchaValidatesPositionWithinTolerance() {
        CaptchaService service = serviceOf(ExtrasWebProperties.Captcha.CaptchaType.SLIDER);

        // 每次校验都是一次性的，故用独立的验证码分别验证三种位置
        Captcha exact = service.generate();
        assertThat(service.validateSlider(exact.id(), Integer.parseInt(exact.code()))).isTrue();

        Captcha within = service.generate();
        int withinX = Integer.parseInt(within.code()) + SliderCaptchaGenerator.TOLERANCE;
        assertThat(service.validateSlider(within.id(), withinX)).isTrue();

        Captcha far = service.generate();
        assertThat(service.validateSlider(far.id(), Integer.parseInt(far.code()) + 100)).isFalse();
    }

    @Test
    void sliderCaptchaIsOneTimeOnly() {
        CaptchaService service = serviceOf(ExtrasWebProperties.Captcha.CaptchaType.SLIDER);
        Captcha captcha = service.generate();
        int gapX = Integer.parseInt(captcha.code());

        assertThat(service.validateSlider(captcha.id(), gapX)).isTrue();
        assertThat(service.validateSlider(captcha.id(), gapX)).isFalse();
    }

    @Test
    void smsRequiresRegisteredSender() {
        CaptchaService service = serviceOf(ExtrasWebProperties.Captcha.CaptchaType.SMS);
        assertThatThrownBy(() -> service.generateSms("13800000000"))
                .isInstanceOf(cn.jowen.framework.extras.common.exception.ExtrasException.class)
                .hasMessageContaining("未注册短信验证码生成器");
    }

    @Test
    void smsGeneratesAndSendsCode() {
        AtomicInteger sentCount = new AtomicInteger();
        SmsCaptchaSender sender = (phone, code) -> sentCount.incrementAndGet();

        CaptchaService service = new CaptchaService(new CaptchaStore.InMemory(),
                List.of(new SmsCaptchaGenerator(6, sender)),
                propsOf(ExtrasWebProperties.Captcha.CaptchaType.SMS));

        Captcha captcha = service.generateSms("13800000000");
        assertThat(sentCount.get()).isEqualTo(1);
        assertThat(captcha.code()).hasSize(6).containsOnlyDigits();
        assertThat(service.validate(captcha.id(), captcha.code())).isTrue();
    }

    @Test
    void generateThrowsWhenTypeNotRegistered() {
        CaptchaService service = new CaptchaService(new CaptchaStore.InMemory(), List.of(),
                propsOf(ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC));

        assertThatThrownBy(service::generate)
                .isInstanceOf(cn.jowen.framework.extras.common.exception.ExtrasException.class)
                .hasMessageContaining("未注册的验证码类型");
    }
}
