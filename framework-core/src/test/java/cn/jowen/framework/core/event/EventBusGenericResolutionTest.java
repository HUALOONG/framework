package cn.jowen.framework.core.event;

import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link EventBus} 监听器泛型解析测试：直接实现（具体类型参数）与匿名实现
 * 均能正确推断事件类型并接收派发。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class EventBusGenericResolutionTest {

    /** 直接实现：泛型参数为具体事件类型，解析必然成功。 */
    static final class ConcreteListener implements EventListener<FrameworkEvent> {
        final AtomicInteger received = new AtomicInteger();

        @Override
        public void onEvent(FrameworkEvent event) {
            received.incrementAndGet();
        }
    }

    @Test
    void directConcreteListener_registersAndReceives() {
        EventBus bus = new EventBus();
        ConcreteListener listener = new ConcreteListener();

        bus.register(listener);
        bus.publish(new FrameworkEvent("src"));

        assertThat(listener.received.get()).isEqualTo(1);
    }

    @Test
    void anonymousListener_concreteType_registersAndReceives() {
        EventBus bus = new EventBus();
        AtomicInteger received = new AtomicInteger();

        bus.register(new EventListener<FrameworkEvent>() {
            @Override
            public void onEvent(FrameworkEvent event) {
                received.incrementAndGet();
            }
        });
        bus.publish(new FrameworkEvent());

        assertThat(received.get()).isEqualTo(1);
    }

    /** 通过中间接口（非参数化）间接继承 {@link EventListener}，触发 Class 分支与嵌套解析。 */
    interface MyEventListener extends EventListener<MyEvent> {
    }

    static final class MyEvent extends FrameworkEvent {
    }

    static final class ViaInterfaceListener implements MyEventListener {
        final AtomicInteger received = new AtomicInteger();

        @Override
        public void onEvent(MyEvent event) {
            received.incrementAndGet();
        }
    }

    /** 泛型继承：父接口携带类型变量，触发类型变量绑定与 {@code resolveType} 解析。 */
    static class GenericListener<E extends FrameworkEvent> implements EventListener<E> {
        final AtomicInteger received = new AtomicInteger();

        @Override
        public void onEvent(E event) {
            received.incrementAndGet();
        }
    }

    static final class ConcreteGeneric extends GenericListener<MyEvent> {
    }

    /** 原始类型（raw）实现 {@link EventListener}，无法解析事件类型。 */
    interface RawEventListener extends EventListener {
    }

    static final class RawListener implements RawEventListener {
        @SuppressWarnings("unused")
        final AtomicInteger received = new AtomicInteger();

        @Override
        public void onEvent(FrameworkEvent event) {
            received.incrementAndGet();
        }
    }

    @Test
    void viaInterfaceListener_resolvesAndReceives() {
        EventBus bus = new EventBus();
        ViaInterfaceListener listener = new ViaInterfaceListener();
        bus.register(listener);
        bus.publish(new MyEvent());
        assertThat(listener.received.get()).isEqualTo(1);
    }

    @Test
    void genericInheritanceListener_registerThrowsSystemException() {
        // 泛型继承（父接口携带类型变量）无法在运行时解析事件类型，注册应抛 SystemException
        EventBus bus = new EventBus();
        ConcreteGeneric listener = new ConcreteGeneric();
        assertThatThrownBy(() -> bus.register(listener))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("无法推断监听器事件类型");
    }

    @Test
    void rawEventListener_registerThrowsSystemException() {
        EventBus bus = new EventBus();
        assertThatThrownBy(() -> bus.register(new RawListener()))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("无法推断监听器事件类型");
    }
}
