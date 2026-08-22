package cn.jowen.framework.core.event;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventBusTest {

    /** 测试事件。 */
    static class TestEvent extends FrameworkEvent {
    }

    /** 测试事件（子类）。 */
    static class SubEvent extends TestEvent {
    }

    /** 泛型父接口，模拟框架层扩展监听器（如 I18nEventListener）。 */
    interface TestEventListener<E extends TestEvent> extends EventListener<E> {
    }

    /** 通过泛型父接口注册，事件类型应能沿继承链解析。 */
    static class RecordingListener implements TestEventListener<TestEvent> {
        final AtomicInteger count = new AtomicInteger();

        @Override
        public void onEvent(TestEvent event) {
            count.incrementAndGet();
        }
    }

    @Test
    void resolvesEventTypeThroughGenericSuperInterface() {
        EventBus bus = new EventBus();
        RecordingListener listener = new RecordingListener();
        bus.register(listener);

        bus.publish(new SubEvent());

        assertThat(listener.count).hasValue(1);
    }

    @Test
    void dispatchesToSubclassEventListeners() {
        EventBus bus = new EventBus();
        RecordingListener listener = new RecordingListener();
        bus.register(listener);

        // 子类事件应命中监听父类事件的监听器
        bus.publish(new SubEvent());
        bus.publish(new TestEvent());

        assertThat(listener.count).hasValue(2);
    }
}
