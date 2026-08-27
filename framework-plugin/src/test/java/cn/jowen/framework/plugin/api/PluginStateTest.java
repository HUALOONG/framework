package cn.jowen.framework.plugin.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginStateTest {

    @Test
    void canTransitTo_createdToStarting_valid() {
        assertThat(PluginState.CREATED.canTransitTo(PluginState.STARTING)).isTrue();
    }

    @Test
    void canTransitTo_createdToDisabled_valid() {
        assertThat(PluginState.CREATED.canTransitTo(PluginState.DISABLED)).isTrue();
    }

    @Test
    void canTransitTo_createdToStarted_invalid() {
        assertThat(PluginState.CREATED.canTransitTo(PluginState.STARTED)).isFalse();
    }

    @Test
    void canTransitTo_startingToStarted_valid() {
        assertThat(PluginState.STARTING.canTransitTo(PluginState.STARTED)).isTrue();
    }

    @Test
    void canTransitTo_startingToFailed_valid() {
        assertThat(PluginState.STARTING.canTransitTo(PluginState.FAILED)).isTrue();
    }

    @Test
    void canTransitTo_startedToStopping_valid() {
        assertThat(PluginState.STARTED.canTransitTo(PluginState.STOPPING)).isTrue();
    }

    @Test
    void canTransitTo_stoppingToStopped_valid() {
        assertThat(PluginState.STOPPING.canTransitTo(PluginState.STOPPED)).isTrue();
    }

    @Test
    void canTransitTo_stoppedToStarting_valid_retry() {
        assertThat(PluginState.STOPPED.canTransitTo(PluginState.STARTING)).isTrue();
    }

    @Test
    void canTransitTo_failedToCreated_valid_retry() {
        assertThat(PluginState.FAILED.canTransitTo(PluginState.CREATED)).isTrue();
    }

    @Test
    void canTransitTo_disabledToCreated_valid_recovery() {
        assertThat(PluginState.DISABLED.canTransitTo(PluginState.CREATED)).isTrue();
    }

    @Test
    void canTransitTo_startedToCreating_invalid() {
        assertThat(PluginState.STARTED.canTransitTo(PluginState.CREATED)).isFalse();
    }

    @Test
    void canTransitTo_stoppedToStopping_invalid() {
        assertThat(PluginState.STOPPED.canTransitTo(PluginState.STOPPING)).isFalse();
    }

    @Test
    void canTransitTo_null_returnsFalse() {
        assertThat(PluginState.CREATED.canTransitTo(null)).isFalse();
    }

    @Test
    void isRunning_started_only() {
        assertThat(PluginState.STARTED.isRunning()).isTrue();
        assertThat(PluginState.CREATED.isRunning()).isFalse();
        assertThat(PluginState.STARTING.isRunning()).isFalse();
        assertThat(PluginState.STOPPING.isRunning()).isFalse();
        assertThat(PluginState.STOPPED.isRunning()).isFalse();
    }

    @Test
    void isTerminal_stoppedAndFailed() {
        assertThat(PluginState.STOPPED.isTerminal()).isTrue();
        assertThat(PluginState.FAILED.isTerminal()).isTrue();
        assertThat(PluginState.STARTED.isTerminal()).isFalse();
        assertThat(PluginState.CREATED.isTerminal()).isFalse();
    }
}
