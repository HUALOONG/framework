package cn.jowen.framework.extras.message.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MessageProperties} 默认值与 setter 契约验证。
 */
class MessagePropertiesTest {

    @Test
    void defaultsAreSensible() {
        MessageProperties properties = new MessageProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getDefaultChannel()).isEqualTo("email");
        assertThat(properties.getMaxRetries()).isZero();
        assertThat(properties.isAsync()).isFalse();
    }

    @Test
    void settersUpdateValues() {
        MessageProperties properties = new MessageProperties();

        properties.setEnabled(false);
        properties.setDefaultChannel("sms");
        properties.setMaxRetries(3);
        properties.setAsync(true);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getDefaultChannel()).isEqualTo("sms");
        assertThat(properties.getMaxRetries()).isEqualTo(3);
        assertThat(properties.isAsync()).isTrue();
    }

    @Test
    void instancesAreIndependent() {
        MessageProperties a = new MessageProperties();
        MessageProperties b = new MessageProperties();

        a.setMaxRetries(5);
        assertThat(b.getMaxRetries()).isZero();
    }
}
