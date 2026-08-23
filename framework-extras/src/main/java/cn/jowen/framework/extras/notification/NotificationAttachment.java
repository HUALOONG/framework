package cn.jowen.framework.extras.notification;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 通知附件。
 *
 * @param name        文件名
 * @param contentType 内容类型（可为 null）
 * @param content     字节内容
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record NotificationAttachment(String name, @Nullable String contentType, byte[] content) {

    public NotificationAttachment {
        Objects.requireNonNull(name, "name must not be null");
        content = content == null ? new byte[0] : content;
    }
}
