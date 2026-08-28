package cn.jowen.framework.extras.message.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MessageTemplate} record 契约验证。
 */
class MessageTemplateTest {

    @Test
    void recordCarriesTypeAndTemplates() {
        MessageTemplate template =
                new MessageTemplate(MessageType.SMS, "标题 ${name}", "内容 ${code}");

        assertThat(template.type()).isEqualTo(MessageType.SMS);
        assertThat(template.titleTemplate()).isEqualTo("标题 ${name}");
        assertThat(template.contentTemplate()).isEqualTo("内容 ${code}");
    }

    @Test
    void equalityIsValueBased() {
        MessageTemplate a = new MessageTemplate(MessageType.EMAIL, "t", "c");
        MessageTemplate b = new MessageTemplate(MessageType.EMAIL, "t", "c");
        MessageTemplate c = new MessageTemplate(MessageType.EMAIL, "t", "x");

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
