package cn.jowen.framework.i18n.event;

import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 语言环境切换事件。当区域解析器将新 Locale 写入上下文（如请求拦截器解析完成）时发布，
 * 供审计、指标或联动业务（如重新查询本地化文案）消费。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class LocaleChangedEvent extends I18nEvent {

    private final Locale previous;
    private final Locale current;

    /**
     * 构造语言切换事件。
     *
     * @param source   切换触发者（可为 {@code null}）
     * @param previous 切换前的区域（可为 {@code null}，表示此前未设置）
     * @param current  切换后的区域（不可为 {@code null}）
     */
    public LocaleChangedEvent(@Nullable Object source, @Nullable Locale previous, Locale current) {
        super(source);
        this.previous = previous;
        this.current = current;
    }

    /** 切换前的区域（可为 {@code null}）。 */
    public @Nullable Locale getPrevious() {
        return previous;
    }

    /** 切换后的区域（不可为 {@code null}）。 */
    public Locale getCurrent() {
        return current;
    }

    @Override
    public String toString() {
        return "LocaleChangedEvent{previous=" + previous + ", current=" + current + '}';
    }
}
