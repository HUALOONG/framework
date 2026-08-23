package cn.jowen.framework.core.event;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 框架事件基类。所有自定义事件应继承此类，提供时间戳与可选的源对象。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public class FrameworkEvent {

    private final Instant timestamp;
    private final @Nullable Object source;

    public FrameworkEvent() {
        this(null);
    }

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
