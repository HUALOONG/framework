package cn.jowen.framework.core.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link LifecycleProcessor} 测试。
 */
class LifecycleProcessorTest {

    @Test
    void add_and_size() {
        LifecycleProcessor proc = new LifecycleProcessor();
        Lifecycle lc = new Lifecycle() {
            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        };
        proc.addLifecycle(lc);
        assertThat(proc.size()).isEqualTo(1);
    }

    @Test
    void addNull_ignored() {
        LifecycleProcessor proc = new LifecycleProcessor();
        proc.addLifecycle(null);
        assertThat(proc.size()).isEqualTo(0);
    }

    @Test
    void removeLifecycle() {
        LifecycleProcessor proc = new LifecycleProcessor();
        Lifecycle lc = new Lifecycle() {
            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        };
        proc.addLifecycle(lc);
        proc.removeLifecycle(lc);
        assertThat(proc.size()).isEqualTo(0);
    }

    @Test
    void startAll_normalLifecycle() {
        LifecycleProcessor proc = new LifecycleProcessor();
        boolean[] initialized = {false};
        proc.addLifecycle(new Lifecycle() {
            @Override
            public void afterPropertiesSet() {
                initialized[0] = true;
            }

            @Override
            public void destroy() {
            }
        });

        proc.startAll();
        assertThat(initialized[0]).isTrue();
    }

    @Test
    void startAll_smartLifecycle_byPhaseAsc() {
        LifecycleProcessor proc = new LifecycleProcessor();
        List<Integer> order = new ArrayList<>();

        SmartLifecycle highPhase = new SmartLifecycle() {
            @Override
            public void start() {
                order.add(getPhase());
            }

            @Override
            public void stop() {
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public int getPhase() {
                return 2;
            }

            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        };

        SmartLifecycle lowPhase = new SmartLifecycle() {
            @Override
            public void start() {
                order.add(getPhase());
            }

            @Override
            public void stop() {
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public int getPhase() {
                return 0;
            }

            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        };

        proc.addLifecycle(highPhase);
        proc.addLifecycle(lowPhase);

        proc.startAll();
        assertThat(order).containsExactly(0, 2);
    }

    @Test
    void startAll_nonAutoStartup_skipped() {
        LifecycleProcessor proc = new LifecycleProcessor();
        boolean[] started = {false};

        proc.addLifecycle(new SmartLifecycle() {
            @Override
            public void start() {
                started[0] = true;
            }

            @Override
            public void stop() {
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public boolean isAutoStartup() {
                return false;
            }

            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        });

        proc.startAll();
        assertThat(started[0]).isFalse();
    }

    @Test
    void stopAll_smartLifecycle_byPhaseDesc() {
        LifecycleProcessor proc = new LifecycleProcessor();
        List<Integer> order = new ArrayList<>();

        proc.addLifecycle(new SmartLifecycle() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
                order.add(getPhase());
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public int getPhase() {
                return 2;
            }

            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        });

        proc.addLifecycle(new SmartLifecycle() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
                order.add(getPhase());
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public int getPhase() {
                return 0;
            }

            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        });

        proc.stopAll();
        assertThat(order).containsExactly(2, 0);
    }

    @Test
    void stopAll_normalLifecycle_destroy() {
        LifecycleProcessor proc = new LifecycleProcessor();
        boolean[] destroyed = {false};

        proc.addLifecycle(new Lifecycle() {
            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
                destroyed[0] = true;
            }
        });

        proc.stopAll();
        assertThat(destroyed[0]).isTrue();
    }

    @Test
    void isRunning_allSmartRunning_returnsTrue() {
        LifecycleProcessor proc = new LifecycleProcessor();
        proc.addLifecycle(new SmartLifecycle() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        });

        assertThat(proc.isRunning()).isTrue();
    }

    @Test
    void isRunning_oneNotRunning_returnsFalse() {
        LifecycleProcessor proc = new LifecycleProcessor();
        proc.addLifecycle(new SmartLifecycle() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public boolean isRunning() {
                return false;
            }

            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        });

        assertThat(proc.isRunning()).isFalse();
    }

    @Test
    void isRunning_noSmartLifecycle_returnsTrue() {
        LifecycleProcessor proc = new LifecycleProcessor();
        proc.addLifecycle(new Lifecycle() {
            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        });

        assertThat(proc.isRunning()).isTrue();
    }

    @Test
    void getLifecycles_returnsCopy() {
        LifecycleProcessor proc = new LifecycleProcessor();
        Lifecycle lc = new Lifecycle() {
            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public void destroy() {
            }
        };
        proc.addLifecycle(lc);
        List<Lifecycle> copy = proc.getLifecycles();
        copy.clear();
        assertThat(proc.size()).isEqualTo(1);
    }
}