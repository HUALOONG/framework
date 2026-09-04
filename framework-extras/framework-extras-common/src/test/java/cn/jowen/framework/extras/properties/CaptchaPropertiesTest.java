package cn.jowen.framework.extras.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CaptchaProperties} 测试：默认值与 setter 覆盖。
 */
class CaptchaPropertiesTest {

    @Test
    void defaults() {
        CaptchaProperties p = new CaptchaProperties();
        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getType()).isEqualTo(CaptchaProperties.CaptchaType.ARITHMETIC);
        assertThat(p.getWidth()).isEqualTo(120);
        assertThat(p.getHeight()).isEqualTo(40);
        assertThat(p.getLength()).isEqualTo(4);
        assertThat(p.getExpireSeconds()).isEqualTo(120L);
    }

    @Test
    void setters() {
        CaptchaProperties p = new CaptchaProperties();
        p.setEnabled(true);
        p.setType(CaptchaProperties.CaptchaType.SMS);
        p.setWidth(200);
        p.setHeight(80);
        p.setLength(6);
        p.setExpireSeconds(300L);
        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getType()).isEqualTo(CaptchaProperties.CaptchaType.SMS);
        assertThat(p.getWidth()).isEqualTo(200);
        assertThat(p.getHeight()).isEqualTo(80);
        assertThat(p.getLength()).isEqualTo(6);
        assertThat(p.getExpireSeconds()).isEqualTo(300L);
    }

    @Test
    void captchaTypeEnum() {
        assertThat(CaptchaProperties.CaptchaType.values()).containsExactly(
                CaptchaProperties.CaptchaType.GRAPHIC,
                CaptchaProperties.CaptchaType.ARITHMETIC,
                CaptchaProperties.CaptchaType.SLIDER,
                CaptchaProperties.CaptchaType.SMS);
    }
}
