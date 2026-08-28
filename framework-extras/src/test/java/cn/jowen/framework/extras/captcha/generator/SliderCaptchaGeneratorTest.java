package cn.jowen.framework.extras.captcha.generator;

import cn.jowen.framework.extras.config.CaptchaProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SliderCaptchaGenerator} 测试：缺口坐标合法、双图非空、answer 与坐标一致。
 */
class SliderCaptchaGeneratorTest {

    @Test
    void generate_returnsPieceAndValidX() {
        SliderCaptchaGenerator generator = new SliderCaptchaGenerator(new CaptchaProperties());
        CaptchaImage result = generator.generate();

        assertThat(result.image()).isNotNull();
        assertThat(result.pieceImage()).isNotNull();
        int x = Integer.parseInt(result.answer());
        int width = new CaptchaProperties().getWidth();
        int pieceSize = Math.max(20, Math.min(40, 40 - 10));
        assertThat(x).isBetween(pieceSize, width - pieceSize - 1);
        assertThat(result.x()).isEqualTo(x);
    }

    @Test
    void generate_pieceSizeMatchesDimensions() {
        CaptchaProperties props = new CaptchaProperties();
        props.setWidth(200);
        props.setHeight(100);
        CaptchaImage result = new SliderCaptchaGenerator(props).generate();
        int pieceSize = Math.max(20, Math.min(40, 100 - 10));
        assertThat(result.pieceImage().getWidth()).isEqualTo(pieceSize);
        assertThat(result.pieceImage().getHeight()).isEqualTo(pieceSize);
    }

    @Test
    void nullProperties_throw() {
        assertThatThrownBy(() -> new SliderCaptchaGenerator(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}