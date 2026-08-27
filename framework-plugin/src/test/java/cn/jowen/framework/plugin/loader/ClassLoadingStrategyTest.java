package cn.jowen.framework.plugin.loader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassLoadingStrategyTest {

    @Test
    void values_containsAllStrategies() {
        assertThat(ClassLoadingStrategy.values()).containsExactlyInAnyOrder(
                ClassLoadingStrategy.PARENT_FIRST,
                ClassLoadingStrategy.CHILD_FIRST,
                ClassLoadingStrategy.FRAMEWORK_API_DELEGATE
        );
    }

    @Test
    void valueOf_byName() {
        assertThat(ClassLoadingStrategy.valueOf("FRAMEWORK_API_DELEGATE"))
                .isEqualTo(ClassLoadingStrategy.FRAMEWORK_API_DELEGATE);
    }
}
