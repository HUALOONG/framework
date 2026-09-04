package cn.jowen.framework.plugin.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 各事件类型到 {@link PluginEvent.EventType} 的映射契约。
 *
 * <p>该映射是外部监听器的分发依据：事件类构造与属性已在 {@link PluginEventTest} 覆盖，
 * 但 {@code getEventType()} 若映射错乱，监听端会静默漏收事件，故单独固化。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class PluginEventTypesTest {

    @Test
    void starting_mapsToStarting() {
        assertThat(new PluginStartingEvent("p").getEventType())
                .isEqualTo(PluginEvent.EventType.STARTING);
    }

    @Test
    void stopping_mapsToStopping() {
        assertThat(new PluginStoppingEvent("p").getEventType())
                .isEqualTo(PluginEvent.EventType.STOPPING);
    }

    @Test
    void stopped_mapsToStopped() {
        assertThat(new PluginStoppedEvent("p").getEventType())
                .isEqualTo(PluginEvent.EventType.STOPPED);
    }

    @Test
    void loaded_mapsToLoaded() {
        assertThat(new PluginLoadedEvent("p").getEventType())
                .isEqualTo(PluginEvent.EventType.LOADED);
    }

    @Test
    void unloaded_mapsToUnloaded() {
        assertThat(new PluginUnloadedEvent("p").getEventType())
                .isEqualTo(PluginEvent.EventType.UNLOADED);
    }

    @Test
    void failed_mapsToFailed() {
        assertThat(new PluginFailedEvent("p", "启动超时").getEventType())
                .isEqualTo(PluginEvent.EventType.FAILED);
    }

    @Test
    void extensionRegistered_mapsToExtensionRegistered() {
        assertThat(new ExtensionRegisteredEvent("p", "ext1", "ep1").getEventType())
                .isEqualTo(PluginEvent.EventType.EXTENSION_REGISTERED);
    }

    @Test
    void extensionUnregistered_mapsToExtensionUnregistered() {
        assertThat(new ExtensionUnregisteredEvent("p", "ext1", "ep1").getEventType())
                .isEqualTo(PluginEvent.EventType.EXTENSION_UNREGISTERED);
    }
}
