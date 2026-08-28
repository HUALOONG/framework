package cn.jowen.framework.i18n.metrics;

import cn.jowen.framework.i18n.api.MessageSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * 国际化消息解析指标收集器：以装饰器方式包装 {@link MessageSource}，统计查找次数、
 * 未命中次数与解析耗时（Micrometer 2.0）。
 *
 * <p>对既有消息源零侵入（无需改动具体实现），Micrometer 为可选依赖，仅在装配时提供
 * {@link MeterRegistry} 才启用。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class I18nMetricsCollector implements MessageSource {

    private static final String COUNTER_LOOKUPS = "framework.i18n.lookups";
    private static final String COUNTER_MISSING = "framework.i18n.missing";
    private static final String TIMER_RESOLUTION = "framework.i18n.resolve";

    private final MessageSource delegate;
    private final Counter lookupCounter;
    private final Counter missingCounter;
    private final Timer resolveTimer;

    /**
     * 构造指标收集器。
     *
     * @param delegate 被包装的消息源，不可为 {@code null}
     * @param registry Micrometer 注册中心，不可为 {@code null}
     */
    public I18nMetricsCollector(MessageSource delegate, MeterRegistry registry) {
        this.delegate = delegate;
        this.lookupCounter = Counter.builder(COUNTER_LOOKUPS)
                .description("i18n message lookups")
                .register(registry);
        this.missingCounter = Counter.builder(COUNTER_MISSING)
                .description("i18n message lookup misses")
                .register(registry);
        this.resolveTimer = Timer.builder(TIMER_RESOLUTION)
                .description("i18n message resolution time")
                .register(registry);
    }

    @Override
    public @Nullable String getMessage(String code, Locale locale, @Nullable Object @Nullable [] args) {
        Timer.Sample sample = Timer.start();
        String result = delegate.getMessage(code, locale, args);
        sample.stop(resolveTimer);
        lookupCounter.increment();
        if (result == null) {
            missingCounter.increment();
        }
        return result;
    }

    @Override
    public boolean contains(String code, Locale locale) {
        return delegate.contains(code, locale);
    }
}