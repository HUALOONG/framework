package cn.jowen.framework.extras.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPropertiesTest {

    @Test
    void gettersAndSetters() {
        NotificationProperties p = new NotificationProperties();
        assertThat(p.isEnabled()).isTrue();
        p.setEnabled(false);
        assertThat(p.isEnabled()).isFalse();

        assertThat(p.getDefaultChannel()).isEqualTo("email");
        p.setDefaultChannel("sms");
        assertThat(p.getDefaultChannel()).isEqualTo("sms");
    }
}
