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
 * {@link GraphicCaptchaGenerator} 测试。
 *
 * <p>覆盖：类型标识、code 长度与字符集（剔除易混字符）、PNG 图像可解码且尺寸符合配置、
 * id 唯一、随机性、过期时间计算。
 */
class GraphicCaptchaGeneratorTest {

    private static final String ALLOWED_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Test
    void type_isGraphic() {
        assertThat(new GraphicCaptchaGenerator(120, 40, 4).type())
                .isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.GRAPHIC);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 6, 8})
    void generate_codeLengthMatchesConfiguration(int length) {
        Captcha captcha = new GraphicCaptchaGenerator(120, 40, length).generate(60);

        assertThat(captcha.code()).hasSize(length);
    }

    @Test
    void generate_codeUsesOnlyUnambiguousCharacters() {
        GraphicCaptchaGenerator generator = new GraphicCaptchaGenerator(120, 40, 6);

        for (int i = 0; i < 100; i++) {
            String code = generator.generate(60).code();
            for (char c : code.toCharArray()) {
                assertThat(ALLOWED_CHARS).as("字符 %s 不在允许字符集内", c).contains(String.valueOf(c));
            }
            // 明确排除易混字符
            assertThat(code).doesNotContain("0").doesNotContain("1").doesNotContain("I").doesNotContain("O");
        }
    }

    @Test
    void generate_producesDecodablePngWithConfiguredSize() throws Exception {
        Captcha captcha = new GraphicCaptchaGenerator(160, 50, 4).generate(60);

        byte[] image = captcha.image();
        assertThat(image).isNotNull().isNotEmpty();
        // PNG 魔数 89 50 4E 47
        assertThat(image[0]).isEqualTo((byte) 0x89);
        assertThat(image[1]).isEqualTo((byte) 0x50);
        assertThat(image[2]).isEqualTo((byte) 0x4E);
        assertThat(image[3]).isEqualTo((byte) 0x47);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(160);
        assertThat(decoded.getHeight()).isEqualTo(50);
    }

    @Test
    void generate_hasNoTextQuestion() {
        assertThat(new GraphicCaptchaGenerator(120, 40, 4).generate(60).text()).isNull();
    }

    @Test
    void generate_idIsUniquePerInvocation() {
        GraphicCaptchaGenerator generator = new GraphicCaptchaGenerator(120, 40, 4);
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            ids.add(generator.generate(60).id());
        }

        assertThat(ids).hasSize(50);
    }

    @Test
    void generate_codesAreRandomAcrossInvocations() {
        GraphicCaptchaGenerator generator = new GraphicCaptchaGenerator(120, 40, 6);
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            codes.add(generator.generate(60).code());
        }

        assertThat(codes).as("6 位随机码在 50 次生成中应有较高多样性").hasSizeGreaterThan(40);
    }

    @Test
    void generate_expireAtReflectsExpireSeconds() {
        long before = System.currentTimeMillis();
        Captcha captcha = new GraphicCaptchaGenerator(120, 40, 4).generate(30);
        long after = System.currentTimeMillis();

        assertThat(captcha.expireAt()).isBetween(before + 30_000L, after + 30_000L);
        assertThat(captcha.expired()).isFalse();
    }

    @Test
    void implementsCaptchaGeneratorContract() {
        assertThat(new GraphicCaptchaGenerator(120, 40, 4)).isInstanceOf(CaptchaGenerator.class);
    }
}
