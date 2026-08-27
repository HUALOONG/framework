package cn.jowen.framework.plugin.resolver;

import cn.jowen.framework.plugin.descriptor.PluginDependency;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.descriptor.VersionRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyResolverTest {

    private DependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DependencyResolver();
    }

    @Test
    void resolve_noDependencies_returnsLoadOrder() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        ResolutionResult result = resolver.resolve(List.of(a));
        assertThat(result.isResolvable()).isTrue();
        assertThat(result.loadOrder()).contains("A");
        assertThat(result.missing()).isEmpty();
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    void resolve_withDependency_orderCorrect() {
        PluginDescriptor a = new PluginDescriptor("A", "A", "1.0", "", "", "",
                "com.example.A",
                List.of(new PluginDependency("B", new VersionRange("[0.0.0,999.999.999]"), false)),
                List.of(), List.of(), List.of(), List.of(), null, true);
        PluginDescriptor b = PluginDescriptor.of("B", "1.0", "com.example.B");
        ResolutionResult result = resolver.resolve(List.of(a, b));
        assertThat(result.isResolvable()).isTrue();
        List<String> order = result.loadOrder();
        assertThat(order.indexOf("A")).isLessThan(order.indexOf("B"));
    }

    @Test
    void resolve_missingDependency_returnsMissing() {
        PluginDescriptor a = new PluginDescriptor("A", "A", "1.0", "", "", "",
                "com.example.A",
                List.of(new PluginDependency("MISSING", new VersionRange("[0.0.0,999.999.999]"), false)),
                List.of(), List.of(), List.of(), List.of(), null, true);
        ResolutionResult result = resolver.resolve(List.of(a));
        assertThat(result.isResolvable()).isFalse();
        assertThat(result.missing()).hasSize(1);
    }

    @Test
    void resolve_versionConflict_returnsConflict() {
        PluginDescriptor a = new PluginDescriptor("A", "A", "1.0", "", "", "",
                "com.example.A",
                List.of(new PluginDependency("B", new VersionRange("[2.0.0,3.0.0]"), false)),
                List.of(), List.of(), List.of(), List.of(), null, true);
        PluginDescriptor b = PluginDescriptor.of("B", "1.0", "com.example.B");
        ResolutionResult result = resolver.resolve(List.of(a, b));
        assertThat(result.isResolvable()).isFalse();
        assertThat(result.conflicts()).hasSize(1);
    }

    @Test
    void resolve_cycleDependency_returnsFailed() {
        PluginDescriptor a = new PluginDescriptor("A", "A", "1.0", "", "", "",
                "com.example.A",
                List.of(new PluginDependency("B", new VersionRange("[0.0.0,999.999.999]"), false)),
                List.of(), List.of(), List.of(), List.of(), null, true);
        PluginDescriptor b = new PluginDescriptor("B", "B", "1.0", "", "", "",
                "com.example.B",
                List.of(new PluginDependency("A", new VersionRange("[0.0.0,999.999.999]"), false)),
                List.of(), List.of(), List.of(), List.of(), null, true);
        ResolutionResult result = resolver.resolve(List.of(a, b));
        assertThat(result.isResolvable()).isFalse();
    }

    @Test
    void resolve_emptyList_returnsEmpty() {
        ResolutionResult result = resolver.resolve(List.of());
        assertThat(result.isResolvable()).isTrue();
        assertThat(result.loadOrder()).isEmpty();
    }

    @Test
    void resolve_transitiveDependencies_orderCorrect() {
        PluginDescriptor a = new PluginDescriptor("A", "A", "1.0", "", "", "",
                "com.example.A",
                List.of(new PluginDependency("B", new VersionRange("[0.0.0,999.999.999]"), false)),
                List.of(), List.of(), List.of(), List.of(), null, true);
        PluginDescriptor b = new PluginDescriptor("B", "B", "1.0", "", "", "",
                "com.example.B",
                List.of(new PluginDependency("C", new VersionRange("[0.0.0,999.999.999]"), false)),
                List.of(), List.of(), List.of(), List.of(), null, true);
        PluginDescriptor c = PluginDescriptor.of("C", "1.0", "com.example.C");
        ResolutionResult result = resolver.resolve(List.of(a, b, c));
        assertThat(result.isResolvable()).isTrue();
        List<String> order = result.loadOrder();
        assertThat(order.indexOf("A")).isLessThan(order.indexOf("B"));
        assertThat(order.indexOf("B")).isLessThan(order.indexOf("C"));
    }
}
