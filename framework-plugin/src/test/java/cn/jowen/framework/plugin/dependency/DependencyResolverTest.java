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

    @Test
    void multipleCallsIndependent() {
        // 第一次调用
        List<PluginDescriptor> first = List.of(
                new PluginDescriptor("a", "1.0.0", "com.a.A", "", List.of()),
                new PluginDescriptor("b", "1.0.0", "com.b.B", "", List.of("a"))
        );
        List<String> order1 = resolver.resolve(first);
        assertThat(order1).containsExactly("a", "b");

        // 第二次调用不同输入，应重新计算
        List<PluginDescriptor> second = List.of(
                new PluginDescriptor("x", "1.0.0", "com.x.X", "", List.of()),
                new PluginDescriptor("y", "1.0.0", "com.y.Y", "", List.of("x"))
        );
        List<String> order2 = resolver.resolve(second);
        assertThat(order2).containsExactly("x", "y");
        // 不应受第一次调用影响
        assertThat(order2).doesNotContain("a", "b");
    }
}
