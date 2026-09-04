package cn.jowen.framework.plugin.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆盖 {@link PluginProperties} 及其内嵌子属性类的全部 getter/setter。
 */
class PluginPropertiesTest {

    @Test
    void gettersAndSetters() {
        PluginProperties p = new PluginProperties();
        assertThat(p.isEnabled()).isTrue();
        p.setEnabled(false);
        assertThat(p.isEnabled()).isFalse();

        assertThat(p.getPluginsDir()).isEqualTo("./plugins");
        p.setPluginsDir("/tmp/plugins");
        assertThat(p.getPluginsDir()).isEqualTo("/tmp/plugins");

        assertThat(p.isAutoStart()).isTrue();
        p.setAutoStart(false);
        assertThat(p.isAutoStart()).isFalse();

        assertThat(p.getDisabledPlugins()).isEmpty();
        p.setDisabledPlugins(List.of("a", "b"));
        assertThat(p.getDisabledPlugins()).containsExactly("a", "b");

        PluginProperties.HotSwapProperties hs = new PluginProperties.HotSwapProperties();
        assertThat(hs.isEnabled()).isTrue();
        assertThat(hs.getStrategy()).isEqualTo("restart");
        assertThat(hs.getDebounceInterval()).isEqualTo("3s");
        hs.setEnabled(false);
        hs.setStrategy("manual");
        hs.setDebounceInterval("5s");
        assertThat(hs.getStrategy()).isEqualTo("manual");
        p.setHotSwap(hs);
        assertThat(p.getHotSwap()).isSameAs(hs);

        PluginProperties.ClassLoadingProperties cl = new PluginProperties.ClassLoadingProperties();
        assertThat(cl.getStrategy()).isEqualTo("framework-api-delegate");
        assertThat(cl.getExportedPackages()).contains("cn.jowen.framework.core.**");
        assertThat(cl.getHiddenClasses()).contains("javax.servlet.**");
        assertThat(cl.getSharedLibraries()).isEmpty();
        cl.setStrategy("s");
        cl.setExportedPackages(List.of("x"));
        cl.setHiddenClasses(List.of("y"));
        cl.setSharedLibraries(List.of("z"));
        assertThat(cl.getSharedLibraries()).containsExactly("z");
        p.setClassLoading(cl);
        assertThat(p.getClassLoading()).isSameAs(cl);

        PluginProperties.SpringProperties sp = new PluginProperties.SpringProperties();
        assertThat(sp.isEnabled()).isFalse();
        assertThat(sp.isParentContextBeanVisibility()).isTrue();
        sp.setEnabled(true);
        sp.setParentContextBeanVisibility(false);
        assertThat(sp.isParentContextBeanVisibility()).isFalse();
        p.setSpring(sp);
        assertThat(p.getSpring()).isSameAs(sp);

        PluginProperties.HealthProperties hp = new PluginProperties.HealthProperties();
        assertThat(hp.isEnabled()).isTrue();
        assertThat(hp.getEndpoint()).isEqualTo("/actuator/plugins");
        hp.setEnabled(false);
        hp.setEndpoint("/h");
        assertThat(hp.getEndpoint()).isEqualTo("/h");
        p.setHealth(hp);
        assertThat(p.getHealth()).isSameAs(hp);
    }
}
