package cn.jowen.framework.core.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link FrameworkEvent} 测试。
 */
class FrameworkEventTest {

    @Test
    void event_defaultConstructor_timestampNotNull() {
        FrameworkEvent event = new FrameworkEvent();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void event_defaultConstructor_sourceNull() {
        FrameworkEvent event = new FrameworkEvent();
        assertThat(event.getSource()).isNull();
    }

    @Test
    void event_sourceConstructor() {
        Object source = new Object();
        FrameworkEvent event = new FrameworkEvent(source);
        assertThat(event.getSource()).isSameAs(source);
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void event_subClassInherits() {
        class OrderEvent extends FrameworkEvent {
            OrderEvent() { super(new Object()); }
        }
        OrderEvent event = new OrderEvent();
        assertThat(event).isInstanceOf(FrameworkEvent.class);
        assertThat(event.getSource()).isNotNull();
    }
}