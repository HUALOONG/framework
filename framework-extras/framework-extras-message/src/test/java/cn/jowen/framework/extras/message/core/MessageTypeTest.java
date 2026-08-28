package cn.jowen.framework.extras.message.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MessageType} 枚举契约验证。
 */
class MessageTypeTest {

    @Test
    void allExpectedTypesPresent() {
        MessageType[] all = MessageType.values();

        assertThat(all).containsExactlyInAnyOrder(
                MessageType.SMS, MessageType.EMAIL, MessageType.SITE,
                MessageType.PUSH, MessageType.DINGTALK,
                MessageType.WECOM, MessageType.WEBHOOK);
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void nameMatchesEnumConstant(MessageType type) {
        assertThat(type.name()).isEqualTo(type.name());
        assertThat(MessageType.valueOf(type.name())).isEqualTo(type);
    }

    @Test
    void enumConstantNamesAreUnique() {
        assertThat(MessageType.values()).doesNotHaveDuplicates();
    }
}
