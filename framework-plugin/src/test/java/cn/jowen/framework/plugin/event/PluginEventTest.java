package cn.jowen.framework.plugin.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginEventTest {

    @Test
    void PluginStartingEvent_pluginId() {
        PluginStartingEvent event = new PluginStartingEvent("my-plugin");
        assertThat(event.getPluginId()).isEqualTo("my-plugin");
    }

    @Test
    void PluginStartedEvent_pluginId() {
        PluginStartedEvent event = new PluginStartedEvent("my-plugin");
        assertThat(event.getPluginId()).isEqualTo("my-plugin");
    }

    @Test
    void PluginStoppingEvent_pluginId() {
        PluginStoppingEvent event = new PluginStoppingEvent("my-plugin");
        assertThat(event.getPluginId()).isEqualTo("my-plugin");
    }

    @Test
    void PluginStoppedEvent_pluginId() {
        PluginStoppedEvent event = new PluginStoppedEvent("my-plugin");
        assertThat(event.getPluginId()).isEqualTo("my-plugin");
    }

    @Test
    void PluginFailedEvent_pluginIdAndMessage() {
        PluginFailedEvent event = new PluginFailedEvent("my-plugin", "启动超时");
        assertThat(event.getPluginId()).isEqualTo("my-plugin");
        assertThat(event.getErrorMessage()).isEqualTo("启动超时");
    }

    @Test
    void PluginLoadedEvent_pluginId() {
        PluginLoadedEvent event = new PluginLoadedEvent("my-plugin");
        assertThat(event.getPluginId()).isEqualTo("my-plugin");
    }

    @Test
    void PluginUnloadedEvent_pluginId() {
        PluginUnloadedEvent event = new PluginUnloadedEvent("my-plugin");
        assertThat(event.getPluginId()).isEqualTo("my-plugin");
    }

    @Test
    void ExtensionRegisteredEvent_extensionIdAndPointId() {
        ExtensionRegisteredEvent event = new ExtensionRegisteredEvent("p1", "ext1", "ep1");
        assertThat(event.getExtensionId()).isEqualTo("ext1");
        assertThat(event.getExtensionPointId()).isEqualTo("ep1");
        assertThat(event.getPluginId()).isEqualTo("p1");
    }

    @Test
    void ExtensionUnregisteredEvent_extensionIdAndPointId() {
        ExtensionUnregisteredEvent event = new ExtensionUnregisteredEvent("p1", "ext1", "ep1");
        assertThat(event.getExtensionId()).isEqualTo("ext1");
        assertThat(event.getExtensionPointId()).isEqualTo("ep1");
        assertThat(event.getPluginId()).isEqualTo("p1");
    }

    @Test
    void timestamp_isSet() {
        PluginStartedEvent event = new PluginStartedEvent("my-plugin");
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void source_isSet() {
        PluginStartedEvent event = new PluginStartedEvent("my-plugin");
        assertThat(event.getSource()).isEqualTo("plugin-started");
    }

    @Test
    void eventType_started() {
        PluginStartedEvent event = new PluginStartedEvent("my-plugin");
        assertThat(event.getEventType()).isEqualTo(PluginEvent.EventType.STARTED);
    }
}
