package cn.jowen.framework.i18n.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link I18nEvent} 与 {@link ResourceLoadFailedEvent} 的补充覆盖测试。
 *
 * <p>覆盖点：
 * <ul>
 *   <li>{@link I18nEvent} 无参构造器：{@code protected} 修饰，供不携带 source 的事件子类复用，
 *       需由同包子类实例化才能触发；</li>
 *   <li>{@link ResourceLoadFailedEvent} 的失败原因存取（含 {@code null} cause）与 {@code toString} 输出。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class I18nEventGapCoverageTest {

    /**
     * 测试专用子类：不携带 source，用于验证基类无参构造器可用且时间戳自动填充。
     */
    private static final class SourcelessEvent extends I18nEvent {
    }

    @Test
    void baseEvent_noArgConstructor_defaultsSourceToNullWithTimestamp() {
        I18nEvent event = new SourcelessEvent();

        assertThat(event.getSource()).isNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void loadFailedEvent_holdsCauseAndSource() {
        RuntimeException cause = new RuntimeException("parse error");
        ResourceLoadFailedEvent event = new ResourceLoadFailedEvent("db-source", cause);

        assertThat(event.getSource()).isEqualTo("db-source");
        assertThat(event.getCause()).isSameAs(cause);
    }

    @Test
    void loadFailedEvent_nullCauseToString_containsSourceAndCause() {
        ResourceLoadFailedEvent event = new ResourceLoadFailedEvent("file-source", null);

        String text = event.toString();

        assertThat(event.getCause()).isNull();
        assertThat(text)
                .startsWith("ResourceLoadFailedEvent{source=")
                .contains("file-source")
                .contains(", cause=null");
    }
}
