package cn.jowen.framework.extras.message.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Message} 构建与取值验证（不可变值对象）。
 */
class MessageTest {

    @Test
    void builderCarriesAllFields() {
        Map<String, Object> attachments = Map.of("file", "a.pdf");
        Message message = Message.builder(MessageType.EMAIL)
                .receiver("user@x.com")
                .title("标题")
                .content("正文")
                .attachments(attachments)
                .build();

        assertThat(message.getType()).isEqualTo(MessageType.EMAIL);
        assertThat(message.getReceiver()).isEqualTo("user@x.com");
        assertThat(message.getTitle()).isEqualTo("标题");
        assertThat(message.getContent()).isEqualTo("正文");
        assertThat(message.getAttachments()).isEqualTo(attachments);
    }

    @Test
    void builderDefaultsTitleAndContentToEmpty() {
        Message message = Message.builder(MessageType.SMS).receiver("138").build();

        assertThat(message.getTitle()).isEmpty();
        assertThat(message.getContent()).isEmpty();
    }

    @Test
    void builderAllowsNullReceiverAndAttachments() {
        Message message = Message.builder(MessageType.SITE)
                .receiver(null)
                .attachments(null)
                .build();

        assertThat(message.getReceiver()).isNull();
        assertThat(message.getAttachments()).isNull();
    }

    @Test
    void messageIsImmutableAfterBuild() {
        Message message = Message.builder(MessageType.PUSH)
                .receiver("r").title("t").content("c").build();

        // 重新构建不应影响已有实例
        Message another = Message.builder(MessageType.PUSH)
                .receiver("r2").title("t2").content("c2").build();

        assertThat(message.getReceiver()).isEqualTo("r");
        assertThat(another.getReceiver()).isEqualTo("r2");
    }

    @Test
    void attachmentsReferenceIsPreservedNotCloned() {
        Map<String, Object> attachments = Map.of("k", 1);
        Message message = Message.builder(MessageType.SMS).attachments(attachments).build();

        assertThat(message.getAttachments()).isSameAs(attachments);
    }
}
