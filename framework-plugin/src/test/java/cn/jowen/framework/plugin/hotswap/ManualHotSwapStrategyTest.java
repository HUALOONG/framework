package cn.jowen.framework.plugin.hotswap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ManualHotSwapStrategy} 单元测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ManualHotSwapStrategyTest {

    @Test
    void collectsChangesWithoutAutoAction() {
        ManualHotSwapStrategy strategy = new ManualHotSwapStrategy();
        strategy.onFileCreated("a.jar");
        strategy.onFileModified("b.jar");
        strategy.onFileDeleted("a.jar");

        assertThat(strategy.pendingChanges()).contains("a.jar", "b.jar");
    }

    @Test
    void clearPending_emptiesList() {
        ManualHotSwapStrategy strategy = new ManualHotSwapStrategy();
        strategy.onFileCreated("a.jar");
        strategy.clearPending();
        assertThat(strategy.pendingChanges()).isEmpty();
    }
}