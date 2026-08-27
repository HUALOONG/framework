package cn.jowen.framework.plugin.lifecycle;

import cn.jowen.framework.plugin.api.PluginState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginStateTransitionTest {

    @Test
    void isValid_createdToStarting_valid() {
        assertThat(PluginStateTransition.isValid(PluginState.CREATED, PluginState.STARTING)).isTrue();
    }

    @Test
    void isValid_createdToStarted_invalid() {
        assertThat(PluginStateTransition.isValid(PluginState.CREATED, PluginState.STARTED)).isFalse();
    }

    @Test
    void isValid_nullTarget_returnsFalse() {
        assertThat(PluginStateTransition.isValid(PluginState.CREATED, null)).isFalse();
    }

    @Test
    void transition_validTransition_succeeds() {
        AtomicReference<PluginState> ref = new AtomicReference<>(PluginState.CREATED);
        PluginStateTransition.transition(ref, PluginState.STARTING);
        assertThat(ref.get()).isEqualTo(PluginState.STARTING);
    }

    @Test
    void transition_invalidTransition_throwsIllegalState() {
        AtomicReference<PluginState> ref = new AtomicReference<>(PluginState.CREATED);
        assertThatThrownBy(() -> PluginStateTransition.transition(ref, PluginState.STARTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("非法状态转换");
    }

    @Test
    void transition_chain_createdToStartingToStarted() {
        AtomicReference<PluginState> ref = new AtomicReference<>(PluginState.CREATED);
        PluginStateTransition.transition(ref, PluginState.STARTING);
        PluginStateTransition.transition(ref, PluginState.STARTED);
        assertThat(ref.get()).isEqualTo(PluginState.STARTED);
    }

    @Test
    void transition_fullLifecycle() {
        AtomicReference<PluginState> ref = new AtomicReference<>(PluginState.CREATED);
        PluginStateTransition.transition(ref, PluginState.STARTING);
        PluginStateTransition.transition(ref, PluginState.STARTED);
        PluginStateTransition.transition(ref, PluginState.STOPPING);
        PluginStateTransition.transition(ref, PluginState.STOPPED);
        PluginStateTransition.transition(ref, PluginState.STARTING);
        assertThat(ref.get()).isEqualTo(PluginState.STARTING);
    }

    @Test
    void transition_toDisabled_fromActiveStates_valid() {
        assertThat(PluginStateTransition.isValid(PluginState.CREATED, PluginState.DISABLED)).isTrue();
        assertThat(PluginStateTransition.isValid(PluginState.STARTING, PluginState.DISABLED)).isTrue();
        assertThat(PluginStateTransition.isValid(PluginState.STARTED, PluginState.DISABLED)).isTrue();
        assertThat(PluginStateTransition.isValid(PluginState.STOPPING, PluginState.DISABLED)).isTrue();
    }
}
