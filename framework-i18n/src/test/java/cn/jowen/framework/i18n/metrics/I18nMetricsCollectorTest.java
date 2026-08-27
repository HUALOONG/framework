package cn.jowen.framework.i18n.metrics;

import cn.jowen.framework.i18n.api.MessageSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link I18nMetricsCollector} 单元测试：验证查找/未命中计数与解析耗时上报。
 *
 * @author 王飞
 * @since 2026-08-27
 */
class I18nMetricsCollectorTest {

    /** 桩消息源：greeting 命中，其余未命中。 */
    private static final MessageSource STUB = new MessageSource() {
        @Override
        public String getMessage(String code, Locale locale, Object[] args) {
            return "greeting".equals(code) ? "hello" : null;
        }

        @Override
        public boolean contains(String code, Locale locale) {
            return "greeting".equals(code);
        }
    };

    @Test
    void recordsLookupsAndMisses() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        I18nMetricsCollector collector = new I18nMetricsCollector(STUB, registry);

        collector.getMessage("greeting", Locale.US, null);
        collector.getMessage("unknown.key", Locale.US, null);

        assertThat(registry.get("framework.i18n.lookups").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("framework.i18n.missing").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("framework.i18n.resolve").timer().count()).isEqualTo(2L);
    }

    @Test
    void containsDelegatesToWrappedSource() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        I18nMetricsCollector collector = new I18nMetricsCollector(STUB, registry);

        assertThat(collector.contains("greeting", Locale.US)).isTrue();
        assertThat(collector.contains("unknown", Locale.US)).isFalse();
    }
}