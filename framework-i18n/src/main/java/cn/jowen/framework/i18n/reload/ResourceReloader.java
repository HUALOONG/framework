package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.core.event.EventBus;
import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import cn.jowen.framework.i18n.event.ResourceLoadFailedEvent;
import cn.jowen.framework.i18n.event.ResourceReloadedEvent;
import java.time.Duration;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 重载执行器。协调 {@link ReloadableMessageSource} 完成资源重载，并在成功/失败时发布事件。
 *
 * <p>重载一致性由消息源实现保证（重载期间继续读取旧快照，完成后原子切换）；
 * 本执行器负责编排与可观测性：成功发布 {@link ResourceReloadedEvent}（含条目数与耗时），
 * 失败发布 {@link ResourceLoadFailedEvent}（含原因）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class ResourceReloader {

    private final @Nullable EventBus eventBus;

    /**
     * 创建不发布事件的重载执行器。
     */
    public ResourceReloader() {
        this(null);
    }

    /**
     * 创建重载执行器。
     *
     * @param eventBus 事件总线，可为 {@code null}（此时仅执行重载，不发布事件）
     */
    public ResourceReloader(@Nullable EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 触发消息源重载。
     *
     * <p>重载期间新请求继续读取旧快照，重载成功后原子切换；失败不切换（保持旧文案可用），
     * 并发布 {@link ResourceLoadFailedEvent}。
     *
     * @param source  消息源，不可为 {@code null}
     * @param locale  本次重载关联的区域，可为 {@code null}（全量重载）
     */
    public void reload(ReloadableMessageSource source, @Nullable Locale locale) {
        long start = System.nanoTime();
        try {
            source.reload();
            publishReloaded(source, locale, start);
        } catch (RuntimeException ex) {
            publishFailed(source, ex);
        }
    }

    /** 便捷重载：区域视为 {@code null}（全量重载）。 */
    public void reload(ReloadableMessageSource source) {
        reload(source, null);
    }

    private void publishReloaded(ReloadableMessageSource source, @Nullable Locale locale, long startNanos) {
        if (eventBus == null) {
            return;
        }
        Duration duration = Duration.ofNanos(System.nanoTime() - startNanos);
        int entryCount = countEntries(source);
        eventBus.publish(new ResourceReloadedEvent(source, locale != null ? locale : Locale.getDefault(), entryCount, duration));
    }

    private void publishFailed(ReloadableMessageSource source, RuntimeException ex) {
        if (eventBus != null) {
            eventBus.publish(new ResourceLoadFailedEvent(source, ex));
        }
    }

    private static int countEntries(ReloadableMessageSource source) {
        // 消息源可选择实现计数能力；未实现时回退为 0（事件中仅作参考）
        if (source instanceof CountableMessageSource countable) {
            return countable.messageCount();
        }
        return 0;
    }

    /** 消息源条目计数扩展点（内部接口，消息源按需实现）。 */
    public interface CountableMessageSource {

        /** 当前缓存的条目总数。 */
        int messageCount();
    }
}
