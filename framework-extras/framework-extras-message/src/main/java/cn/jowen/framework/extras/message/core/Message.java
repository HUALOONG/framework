package cn.jowen.framework.extras.message.core;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 待发送消息的不可变描述。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Message {

    private final MessageType type;
    private final @Nullable String receiver;
    private final String title;
    private final String content;
    private final @Nullable Map<String, Object> attachments;

    private Message(Builder builder) {
        this.type = builder.type;
        this.receiver = builder.receiver;
        this.title = builder.title;
        this.content = builder.content;
        this.attachments = builder.attachments;
    }

    public static Builder builder(MessageType type) {
        return new Builder(type);
    }

    public MessageType getType() {
        return type;
    }

    public @Nullable String getReceiver() {
        return receiver;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public @Nullable Map<String, Object> getAttachments() {
        return attachments;
    }

    public static final class Builder {
        private final MessageType type;
        private @Nullable String receiver;
        private String title = "";
        private String content = "";
        private @Nullable Map<String, Object> attachments;

        private Builder(MessageType type) {
            this.type = type;
        }

        public Builder receiver(@Nullable String receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder attachments(@Nullable Map<String, Object> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}
