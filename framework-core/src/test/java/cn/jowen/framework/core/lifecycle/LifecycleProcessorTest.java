package cn.jowen.framework.core.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link LifecycleProcessor} 生命周期处理器测试。
 */
class LifecycleProcessorTest {

    @Test
    void startAll_ordersByPhaseAsc() {
        List<String> events = new ArrayList<>();
        LifecycleProcessor processor = new LifecycleProcessor();
        processor.addLifecycle(new RecordingSmart(10, "slow", events));
        processor.addLifecycle(new RecordingSmart(-10, "fast", events));
        processor.addLifecycle(new RecordingSmart(0, "middle", events));

        processor.startAll();

        assertThat(events).containsExactly("start:fast", "start:middle", "start:slow");
    }

    @Test
    void stopAll_ordersByPhaseDesc() {
        List<String> events = new ArrayList<>();
        LifecycleProcessor processor = new LifecycleProcessor();
        processor.addLifecycle(new RecordingSmart(10, "slow", events));
        processor.addLifecycle(new RecordingSmart(-10, "fast", events));

        processor.stopAll();

        assertThat(events).containsExactly("stop:slow", "stop:fast");
    }

    @Test
    void nonSmart_initializedAndDestroyed() {
        List<String> events = new ArrayList<>();
        LifecycleProcessor processor = new LifecycleProcessor();
        processor.addLifecycle(new RecordingInit("bean", events));

        processor.startAll();
        processor.stopAll();

        assertThat(events).containsExactly("init:bean", "destroy:bean");
    }

    @Test
    void autoStartupFalse_notStartedOnStartAll() {
        List<String> events = new ArrayList<>();
        LifecycleProcessor processor = new LifecycleProcessor();
        processor.addLifecycle(new RecordingSmart(0, "manual", events) {
            @Override
            public boolean isAutoStartup() {
                return false;
            }
        });

        processor.startAll();

        assertThat(events).isEmpty();
    }

    @Test
    void isRunning_reflectsAllSmart() {
        LifecycleProcessor processor = new LifecycleProcessor();
        processor.addLifecycle(new RecordingSmart(0, "a", new ArrayList<>()));
        processor.addLifecycle(new RecordingSmart(0, "b", new ArrayList<>()));
        assertThat(processor.isRunning()).isFalse();

        processor.startAll();
        assertThat(processor.isRunning()).isTrue();

        processor.stopAll();
        assertThat(processor.isRunning()).isFalse();
    }

    /** 记录事件的 SmartLifecycle。 */
    static class RecordingSmart implements SmartLifecycle {
        private final int phase;
        private final String name;
        private final List<String> events;
        private boolean running;

        RecordingSmart(int phase, String name, List<String> events) {
            this.phase = phase;
            this.name = name;
            this.events = events;
        }

        @Override
        public void start() {
            running = true;
            events.add("start:" + name);
        }

        @Override
        public void stop() {
            running = false;
            events.add("stop:" + name);
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public int getPhase() {
            return phase;
        }

        @Override
        public void afterPropertiesSet() {
            // 初始化无额外动作
        }

        @Override
        public void destroy() {
            // 销毁无额外动作
        }
    }

    /** 记录事件的普通 Lifecycle（仅初始化/销毁）。 */
    static class RecordingInit implements Lifecycle {
        private final String name;
        private final List<String> events;

        RecordingInit(String name, List<String> events) {
            this.name = name;
            this.events = events;
        }

        @Override
        public void afterPropertiesSet() {
            events.add("init:" + name);
        }

        @Override
        public void destroy() {
            events.add("destroy:" + name);
        }
    }
}
