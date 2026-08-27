package cn.jowen.framework.plugin.config;

import cn.jowen.framework.plugin.config.PluginProperties;
import cn.jowen.framework.plugin.config.PluginProperties.ClassLoadingProperties;
import cn.jowen.framework.plugin.config.PluginProperties.HotSwapProperties;
import cn.jowen.framework.plugin.config.PluginProperties.HealthProperties;
import cn.jowen.framework.plugin.config.PluginProperties.SpringProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginPropertiesTest {

    private PluginProperties props;

    @BeforeEach
    void setUp() {
        props = new PluginProperties();
    }

    @Test
    void defaults() {
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getPluginsDir()).isEqualTo("./plugins");
        assertThat(props.isAutoStart()).isTrue();
        assertThat(props.getDisabledPlugins()).isEmpty();
    }

    @Test
    void setPluginsDir() {
        props.setPluginsDir("/opt/plugins");
        assertThat(props.getPluginsDir()).isEqualTo("/opt/plugins");
    }

    @Test
    void setEnabled() {
        props.setEnabled(false);
        assertThat(props.isEnabled()).isFalse();
    }

    @Test
    void setAutoStart() {
        props.setAutoStart(false);
        assertThat(props.isAutoStart()).isFalse();
    }

    @Test
    void hotSwapDefaults() {
        HotSwapProperties hs = props.getHotSwap();
        assertThat(hs.isEnabled()).isTrue();
        assertThat(hs.getStrategy()).isEqualTo("restart");
        assertThat(hs.getDebounceInterval()).isEqualTo("3s");
    }

    @Test
    void classLoadingDefaults() {
        ClassLoadingProperties cl = props.getClassLoading();
        assertThat(cl.getStrategy()).isEqualTo("framework-api-delegate");
        assertThat(cl.getExportedPackages()).hasSize(2);
        assertThat(cl.getHiddenClasses()).contains("javax.servlet.**");
        assertThat(cl.getSharedLibraries()).isEmpty();
    }

    @Test
    void springDefaults() {
        SpringProperties sp = props.getSpring();
        assertThat(sp.isEnabled()).isFalse();
        assertThat(sp.isParentContextBeanVisibility()).isTrue();
    }

    @Test
    void healthDefaults() {
        HealthProperties hp = props.getHealth();
        assertThat(hp.isEnabled()).isTrue();
        assertThat(hp.getEndpoint()).isEqualTo("/actuator/plugins");
    }
}
