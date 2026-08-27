package cn.jowen.framework.core.event;

import cn.jowen.framework.core.exception.SystemException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link EventBus} 测试。
 */
class EventBusTest {

    static class OrderEvent extends FrameworkEvent {
        final String orderId;

        OrderEvent(String orderId) {
            super();
            this.orderId = orderId;
        }
    }

    static class PaymentEvent extends OrderEvent {
        PaymentEvent(String orderId) {
            super(orderId);
        }
    }

    static class SubscribableListener implements EventListener<OrderEvent> {
        final List<OrderEvent> events = new ArrayList<>();

        @Override
        public void onEvent(@NonNull OrderEvent event) {
            events.add(event);
        }
    }

    @Test
    void register_and_publish_sync() {
        EventBus bus = new EventBus();
        SubscribableListener listener = new SubscribableListener();
        bus.register(listener);

        bus.publish(new OrderEvent("o1"));
        bus.publish(new OrderEvent("o2"));

        assertThat(listener.events).hasSize(2);
        assertThat(listener.events.getFirst().orderId).isEqualTo("o1");
    }

    @Test
    void publish_toParentTypeListener() {
        EventBus bus = new EventBus();
        SubscribableListener listener = new SubscribableListener();
        bus.register(listener);

        bus.publish(new PaymentEvent("o1"));
        assertThat(listener.events).hasSize(1);
        assertThat(listener.events.getFirst().orderId).isEqualTo("o1");
    }

    @Test
    void multipleListeners() {
        EventBus bus = new EventBus();
        SubscribableListener l1 = new SubscribableListener();
        SubscribableListener l2 = new SubscribableListener();
        bus.register(l1);
        bus.register(l2);

        bus.publish(new OrderEvent("o1"));
        assertThat(l1.events).hasSize(1);
        assertThat(l2.events).hasSize(1);
    }

    @Test
    void publish_async() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        EventBus bus = new EventBus(executor);
        SubscribableListener listener = new SubscribableListener();
        bus.register(listener);

        CountDownLatch latch = new CountDownLatch(1);
        bus.publish(new OrderEvent("o1"));
        latch.await(2, TimeUnit.SECONDS);

        assertThat(listener.events).hasSize(1);
        executor.shutdownNow();
    }

    @Test
    void listenerThrows_wrappedInSystemException() {
        EventBus bus = new EventBus();
        bus.register(new EventListener<OrderEvent>() {
            @Override
            public void onEvent(@NonNull OrderEvent event) {
                throw new RuntimeException("boom");
            }
        });

        assertThatThrownBy(() -> bus.publish(new OrderEvent("o1")))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("事件监听器执行失败");
    }
}
