package cn.jowen.framework.plugin.dependency;

import cn.jowen.framework.plugin.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyResolverTest {

    private final DependencyResolver resolver = new DependencyResolver();

    @Test
    void noDependencies() {
        List<PluginDescriptor> descriptors = List.of(
                PluginDescriptor.of("a", "1.0.0", "com.a.A"),
                PluginDescriptor.of("b", "1.0.0", "com.b.B")
        );
        List<String> order = resolver.resolve(descriptors);
        assertThat(order).containsExactly("a", "b");
    }

    @Test
    void dependentPluginsOrder() {
        List<PluginDescriptor> descriptors = List.of(
                PluginDescriptor.of("a", "1.0.0", "com.a.A"),
                PluginDescriptor.of("b", "1.0.0", "com.b.B", List.of("a"))
        );
        List<String> order = resolver.resolve(descriptors);
        assertThat(order.indexOf("a")).isLessThan(order.indexOf("b"));
    }

    @Test
    void chainDependency() {
        List<PluginDescriptor> descriptors = List.of(
                PluginDescriptor.of("a", "1.0.0", "com.a.A"),
                PluginDescriptor.of("b", "1.0.0", "com.b.B", List.of("a")),
                PluginDescriptor.of("c", "1.0.0", "com.c.C", List.of("b"))
        );
        List<String> order = resolver.resolve(descriptors);
        assertThat(order).containsExactly("a", "b", "c");
    }

    @Test
    void missingDependencyRejected() {
        List<PluginDescriptor> descriptors = List.of(
                new PluginDescriptor("a", "1.0.0", "com.a.A", "", List.of("missing"))
        );
        assertThatThrownBy(() -> resolver.resolve(descriptors))
                .isInstanceOf(DependencyResolutionException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void circularDependencyRejected() {
        List<PluginDescriptor> descriptors = List.of(
                new PluginDescriptor("a", "1.0.0", "com.a.A", "", List.of("b")),
                new PluginDescriptor("b", "1.0.0", "com.b.B", "", List.of("a"))
        );
        assertThatThrownBy(() -> resolver.resolve(descriptors))
                .isInstanceOf(DependencyResolutionException.class)
                .hasMessageContaining("循环");
    }
}
