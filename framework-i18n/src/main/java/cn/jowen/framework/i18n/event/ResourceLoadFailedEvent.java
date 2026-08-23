package cn.jowen.framework.i18n.event;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 资源加载失败事件。热加载过程中底层资源读取/解析抛错时发布，携带失败原因供告警与排障。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public class ResourceLoadFailedEvent extends I18nEvent {

    private final Throwable cause;

    /**
     * 构造加载失败事件。
     *
     * @param source 失败的消息源（不可为 {@code null}）
     * @param cause  失败原因（可为 {@code null}）
     */
    public ResourceLoadFailedEvent(Object source, @Nullable Throwable cause) {
        super(source);
        this.cause = cause;
    }

    /**
     * 失败原因（可为 {@code null}）。
     */
    public @Nullable Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "ResourceLoadFailedEvent{source=" + getSource() + ", cause=" + cause + '}';
    }
}
