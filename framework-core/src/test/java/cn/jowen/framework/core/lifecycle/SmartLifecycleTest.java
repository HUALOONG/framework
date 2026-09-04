package cn.jowen.framework.core.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SmartLifecycle} 默认方法（{@link SmartLifecycle#getPhase()}、
 * {@link SmartLifecycle#isAutoStartup()}、{@link SmartLifecycle#stop(Runnable)}）覆盖测试。
 */
class SmartLifecycleTest {

    /**
     * 仅实现抽象方法的最小实现，默认值由接口默认方法提供。
     */
    static class MinimalLifecycle implements SmartLifecycle {
        private boolean running = false;

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public void afterPropertiesSet() {
            // 无需初始化逻辑
        }

        @Override
        public void destroy() {
            // 无需销毁逻辑
        }
    }

    @Test
    void defaultMethods_behaveAsSpecified() {
        MinimalLifecycle lifecycle = new MinimalLifecycle();

        // getPhase() 默认 0
        assertThat(lifecycle.getPhase()).isEqualTo(0);

        // isAutoStartup() 默认 true
        assertThat(lifecycle.isAutoStartup()).isTrue();

        // stop(Runnable) 默认：先 stop()，再执行回调
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();
        lifecycle.stop(() -> callbackInvoked.set(true));
        assertThat(lifecycle.isRunning()).isFalse();
        assertThat(callbackInvoked).isTrue();
    }
}
