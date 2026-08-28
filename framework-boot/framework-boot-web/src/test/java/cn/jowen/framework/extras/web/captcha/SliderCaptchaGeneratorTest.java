package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SliderCaptchaGenerator} 测试。
 *
 * <p>覆盖：类型标识、缺口坐标落在可绘制范围内、PNG 图像可解码、提示文案、
 * validatePosition 的容差边界与非法输入。
 */
class SliderCaptchaGeneratorTest {

    @Test
    void type_isSlider() {
        assertThat(new SliderCaptchaGenerator(200, 100, 40).type())
                .isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.SLIDER);
    }

    @Test
    void tolerance_isFivePixels() {
        assertThat(SliderCaptchaGenerator.TOLERANCE).isEqualTo(5);
    }

    @Test
    void generate_gapXStaysWithinDrawableRange() {
        int width = 300;
        int blockSize = 40;
        SliderCaptchaGenerator generator = new SliderCaptchaGenerator(width, 150, blockSize);

        for (int i = 0; i < 200; i++) {
            int gapX = Integer.parseInt(generator.generate(60).code());
            assertThat(gapX)
                    .isGreaterThanOrEqualTo(blockSize + 10)
                    .isLessThan(width - blockSize - 10);
        }
    }

    @Test
    void generate_codeIsNumericGapCoordinate() {
        Captcha captcha = new SliderCaptchaGenerator(200, 100, 40).generate(60);

        assertThat(captcha.code()).containsOnlyDigits();
    }

    @Test
    void generate_providesUserHintText() {
        assertThat(new SliderCaptchaGenerator(200, 100, 40).generate(60).text())
                .isEqualTo("请拖动滑块拼合缺口");
    }

    @Test
    void generate_producesDecodablePngWithConfiguredSize() throws Exception {
        Captcha captcha = new SliderCaptchaGenerator(240, 120, 40).generate(60);

        byte[] image = captcha.image();
        assertThat(image).isNotNull().isNotEmpty();
        assertThat(image[0]).isEqualTo((byte) 0x89);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(240);
        assertThat(decoded.getHeight()).isEqualTo(120);
    }

    @Test
    void generate_gapCoordinatesVaryAcrossInvocations() {
        SliderCaptchaGenerator generator = new SliderCaptchaGenerator(300, 150, 40);
        Set<String> gaps = new HashSet<>();

        for (int i = 0; i < 60; i++) {
            gaps.add(generator.generate(60).code());
        }

        assertThat(gaps).as("缺口位置应随机").hasSizeGreaterThan(10);
    }

    @Test
    void generate_expireAtReflectsExpireSeconds() {
        long before = System.currentTimeMillis();
        Captcha captcha = new SliderCaptchaGenerator(200, 100, 40).generate(45);
        long after = System.currentTimeMillis();

        assertThat(captcha.expireAt()).isBetween(before + 45_000L, after + 45_000L);
    }

    // ---------- validatePosition ----------

    @Test
    void validatePosition_acceptsExactMatch() {
        assertThat(SliderCaptchaGenerator.validatePosition("100", 100)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {95, 96, 99, 100, 101, 104, 105})
    void validatePosition_acceptsWithinTolerance(int actualX) {
        assertThat(SliderCaptchaGenerator.validatePosition("100", actualX)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {94, 106, 0, 200, -100})
    void validatePosition_rejectsOutsideTolerance(int actualX) {
        assertThat(SliderCaptchaGenerator.validatePosition("100", actualX)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "10.5", "1 0", " 100", "9999999999999999999"})
    void validatePosition_rejectsNonIntegerExpectedValue(String expectedX) {
        assertThat(SliderCaptchaGenerator.validatePosition(expectedX, 100)).isFalse();
    }

    @Test
    void validatePosition_rejectsNullExpectedValue() {
        assertThat(SliderCaptchaGenerator.validatePosition(null, 100)).isFalse();
    }

    @Test
    void validatePosition_toleranceIsSymmetric() {
        int expected = 50;
        assertThat(SliderCaptchaGenerator.validatePosition(String.valueOf(expected),
                expected - SliderCaptchaGenerator.TOLERANCE)).isTrue();
        assertThat(SliderCaptchaGenerator.validatePosition(String.valueOf(expected),
                expected + SliderCaptchaGenerator.TOLERANCE)).isTrue();
        assertThat(SliderCaptchaGenerator.validatePosition(String.valueOf(expected),
                expected - SliderCaptchaGenerator.TOLERANCE - 1)).isFalse();
        assertThat(SliderCaptchaGenerator.validatePosition(String.valueOf(expected),
                expected + SliderCaptchaGenerator.TOLERANCE + 1)).isFalse();
    }

    @Test
    void implementsCaptchaGeneratorContract() {
        assertThat(new SliderCaptchaGenerator(200, 100, 40)).isInstanceOf(CaptchaGenerator.class);
    }
}
