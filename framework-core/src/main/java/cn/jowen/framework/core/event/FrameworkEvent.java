package cn.jowen.framework.core.event;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 框架事件基类。所有自定义事件应继承此类，提供时间戳与可选的源对象。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class FrameworkEvent {
    /**
     * 事件发生时间。
     */
    private final Instant timestamp;

    /**
     * 事件源对象（可为 {@code null}）。
     */
    private final @Nullable Object source;

    /**
     * 创建事件。
     */
    public FrameworkEvent() {
        this(null);
    }

    /**
     * 创建事件。
     *
     * @param source 事件源对象，可为 {@code null}
     */
    public FrameworkEvent(@Nullable Object source) {
        this.timestamp = Instant.now();
        this.source = source;
    }

    /**
     * 返回事件发生时间。
     *
     * @return 时间戳，不可为 {@code null}
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * 返回事件源（可为 {@code null}）。
     *
     * @return 事件源
     */
    public @Nullable Object getSource() {
        return source;
    }
}
