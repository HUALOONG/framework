package cn.jowen.framework.plugin.descriptor;

import cn.jowen.framework.plugin.support.ValidationError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

class PluginDescriptorTest {

    @Test
    void of_createsDescriptor() {
        PluginDescriptor desc = PluginDescriptor.of("my-plugin", "1.0.0", "com.example.MyPlugin");
        assertThat(desc.pluginId()).isEqualTo("my-plugin");
        assertThat(desc.version()).isEqualTo("1.0.0");
        assertThat(desc.pluginClass()).isEqualTo("com.example.MyPlugin");
    }

    @Test
    void constructor_singleParam() {
        PluginDescriptor desc = new PluginDescriptor("simple");
        assertThat(desc.pluginId()).isEqualTo("simple");
        assertThat(desc.version()).isEqualTo("0.0.0");
    }

    @Test
    void validate_validDescriptor_noErrors() {
        PluginDescriptor desc = PluginDescriptor.of("my-plugin", "1.0.0", "com.example.MyPlugin");
        assertThat(desc.validate()).isEmpty();
    }

    @Test
    void validate_nullPluginId_addsError() {
        PluginDescriptor desc = new PluginDescriptor(null, "n", "1.0", "", "", "", "com.example.X",
                List.of(), List.of(), List.of(), List.of(), List.of(), null, true);
        assertThat(desc.validate()).hasSize(1);
        assertThat(desc.validate().getFirst().field()).isEqualTo("pluginId");
    }

    @Test
    void validate_blankPluginId_addsError() {
        PluginDescriptor desc = new PluginDescriptor("", "n", "1.0", "", "", "", "com.example.X",
                List.of(), List.of(), List.of(), List.of(), List.of(), null, true);
        assertThat(desc.validate()).hasSize(1);
        assertThat(desc.validate().getFirst().field()).isEqualTo("pluginId");
    }

    @Test
    void validate_blankVersion_addsError() {
        PluginDescriptor desc = new PluginDescriptor("p", "n", "", "", "", "", "c",
                List.of(), List.of(), List.of(), List.of(), List.of(), null, true);
        assertThat(desc.validate()).hasSize(1);
        assertThat(desc.validate().getFirst().field()).isEqualTo("version");
    }

    @Test
    void validate_blankPluginClass_addsError() {
        PluginDescriptor desc = new PluginDescriptor("p", "n", "1.0", "", "", "", "",
                List.of(), List.of(), List.of(), List.of(), List.of(), null, true);
        assertThat(desc.validate()).hasSize(1);
        assertThat(desc.validate().getFirst().field()).isEqualTo("pluginClass");
    }

    @Test
    void validate_multipleErrors() {
        PluginDescriptor desc = new PluginDescriptor("", "n", "", "", "", "", "",
                List.of(), List.of(), List.of(), List.of(), List.of(), null, true);
        assertThat(desc.validate()).hasSize(3);
    }

    @Test
    void constructor_defaults_enabledByDefault() {
        PluginDescriptor desc = new PluginDescriptor("p");
        assertThat(desc.enabledByDefault()).isTrue();
    }

    @Test
    void defaults_emptyLists() {
        PluginDescriptor desc = new PluginDescriptor("p");
        assertThat(desc.requires()).isEmpty();
        assertThat(desc.optionalRequires()).isEmpty();
        assertThat(desc.provides()).isEmpty();
        assertThat(desc.extensionPoints()).isEmpty();
        assertThat(desc.extensions()).isEmpty();
        assertThat(desc.configuration()).isNull();
    }
}
