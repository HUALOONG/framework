package cn.jowen.framework.extras.message.template;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Template} 取值验证。
 */
class TemplateTest {

    @Test
    void constructorExposesCodeAndTemplates() {
        Template template = new Template("REGISTER", "欢迎 ${name}", "您的验证码 ${code}");

        assertThat(template.getCode()).isEqualTo("REGISTER");
        assertThat(template.getTitleTemplate()).isEqualTo("欢迎 ${name}");
        assertThat(template.getContentTemplate()).isEqualTo("您的验证码 ${code}");
    }

    @Test
    void nullOrEmptyTemplatesArePreserved() {
        Template template = new Template("X", "", null);

        assertThat(template.getCode()).isEqualTo("X");
        assertThat(template.getTitleTemplate()).isEmpty();
        assertThat(template.getContentTemplate()).isNull();
    }
}
