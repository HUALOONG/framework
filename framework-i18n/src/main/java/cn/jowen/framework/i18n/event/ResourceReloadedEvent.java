package cn.jowen.framework.i18n.event;

import java.time.Duration;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 资源重载完成事件。由热加载器（如 {@code FileWatchResourceWatcher}）在重载成功后发布，
 * 携带受影响的区域与重载耗时，供监控与可观测性消费。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class ResourceReloadedEvent extends I18nEvent {

    private final Locale locale;
    private final int entryCount;
    private final Duration duration;

    /**
     * 构造重载完成事件。
     *
     * @param source     重载的消息源（不可为 {@code null}）
     * @param locale     本次受影响的区域（不可为 {@code null}）
     * @param entryCount 重载后的条目数
     * @param duration   重载耗时
     */
    public ResourceReloadedEvent(Object source, Locale locale, int entryCount, Duration duration) {
        super(source);
        this.locale = locale;
        this.entryCount = entryCount;
        this.duration = duration;
    }

    /** 本次受影响的区域。 */
    public Locale getLocale() {
        return locale;
    }

    /** 重载后的条目数。 */
    public int getEntryCount() {
        return entryCount;
    }

    /** 重载耗时。 */
    public Duration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "ResourceReloadedEvent{locale=" + locale + ", entryCount=" + entryCount
                + ", duration=" + duration + ", source=" + getSource() + '}';
    }
}
