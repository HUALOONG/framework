package cn.jowen.framework.extras.notification;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通知请求。
 *
 * @param channel        渠道（不可为 null）
 * @param to             接收方（邮箱/手机号/Webhook 地址等）
 * @param subject        标题（可为 null）
 * @param content        内容
 * @param templateCode   模板编码（可为 null）
 * @param templateParams 模板参数（可为 null）
 * @param attachments    附件列表（可为 null）
 * @param extra          扩展字段（可为 null）
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record NotificationRequest(NotificationChannel channel, @Nullable String to, @Nullable String subject,
                                  @Nullable String content, @Nullable String templateCode,
                                  @Nullable Map<String, Object> templateParams,
                                  @Nullable List<NotificationAttachment> attachments,
                                  @Nullable Map<String, Object> extra) {

    public NotificationRequest {
        Objects.requireNonNull(channel, "channel must not be null");
        templateParams = templateParams == null ? Map.of() : templateParams;
        attachments = attachments == null ? List.of() : attachments;
        extra = extra == null ? Map.of() : extra;
    }

    /**
     * 便捷构建器。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @author 王飞
     */
    public static final class Builder {
        private NotificationChannel channel;
        private String to;
        private String subject;
        private String content;
        private String templateCode;
        private Map<String, Object> templateParams;
        private List<NotificationAttachment> attachments;
        private Map<String, Object> extra;

        public Builder channel(NotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder templateCode(String templateCode) {
            this.templateCode = templateCode;
            return this;
        }

        public Builder templateParam(String key, Object value) {
            if (this.templateParams == null) {
                this.templateParams = new HashMap<>();
            }
            this.templateParams.put(key, value);
            return this;
        }

        public Builder attachment(NotificationAttachment attachment) {
            if (this.attachments == null) {
                this.attachments = new ArrayList<>();
            }
            this.attachments.add(attachment);
            return this;
        }

        public Builder extra(String key, Object value) {
            if (this.extra == null) {
                this.extra = new HashMap<>();
            }
            this.extra.put(key, value);
            return this;
        }

        public NotificationRequest build() {
            Objects.requireNonNull(channel, "channel must not be null");
            return new NotificationRequest(channel, to, subject, content, templateCode,
                    templateParams, attachments, extra);
        }
    }
}
