package cn.jowen.framework.plugin.hotswap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HotSwapStrategyTest {

    @Test
    void values_containsAllStrategies() {
        assertThat(HotSwapStrategy.values()).containsExactlyInAnyOrder(
                HotSwapStrategy.RESTART,
                HotSwapStrategy.RELOAD_CLASSES,
                HotSwapStrategy.MANUAL
        );
    }

    @Test
    void valueOf_byName() {
        assertThat(HotSwapStrategy.valueOf("RESTART")).isEqualTo(HotSwapStrategy.RESTART);
        assertThat(HotSwapStrategy.valueOf("MANUAL")).isEqualTo(HotSwapStrategy.MANUAL);
    }
}
