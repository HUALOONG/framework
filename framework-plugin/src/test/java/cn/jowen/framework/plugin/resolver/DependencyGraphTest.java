package cn.jowen.framework.plugin.resolver;

import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

class DependencyGraphTest {

    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
    }

    @Test
    void addNode_andAddEdge_basicStructure() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        graph.addNode("A", a);
        graph.addEdge("A", "B");
        assertThat(graph.getNodes()).containsKey("A");
    }

    @Test
    void topologicalSort_linearOrder() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        PluginDescriptor b = PluginDescriptor.of("B", "1.0", "com.example.B");
        PluginDescriptor c = PluginDescriptor.of("C", "1.0", "com.example.C");
        graph.addNode("A", a);
        graph.addNode("B", b);
        graph.addNode("C", c);
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        List<String> order = graph.topologicalSort();
        assertThat(order).contains("A", "B", "C");
        assertThat(order.indexOf("A")).isLessThan(order.indexOf("B"));
        assertThat(order.indexOf("B")).isLessThan(order.indexOf("C"));
    }

    @Test
    void topologicalSort_cycle_throwsCycleDetected() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        PluginDescriptor b = PluginDescriptor.of("B", "1.0", "com.example.B");
        graph.addNode("A", a);
        graph.addNode("B", b);
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        assertThatThrownBy(graph::topologicalSort)
                .isInstanceOf(DependencyGraph.CycleDetectedException.class);
    }

    @Test
    void detectCycles_returnsTrueForCycle() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        PluginDescriptor b = PluginDescriptor.of("B", "1.0", "com.example.B");
        graph.addNode("A", a);
        graph.addNode("B", b);
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        assertThat(graph.detectCycles()).isTrue();
    }

    @Test
    void detectCycles_returnsFalseForNoCycle() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        PluginDescriptor b = PluginDescriptor.of("B", "1.0", "com.example.B");
        graph.addNode("A", a);
        graph.addNode("B", b);
        graph.addEdge("A", "B");
        assertThat(graph.detectCycles()).isFalse();
    }

    @Test
    void getTransitiveDependencies_returnsAllDescendants() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        PluginDescriptor b = PluginDescriptor.of("B", "1.0", "com.example.B");
        PluginDescriptor c = PluginDescriptor.of("C", "1.0", "com.example.C");
        graph.addNode("A", a);
        graph.addNode("B", b);
        graph.addNode("C", c);
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        assertThat(graph.getTransitiveDependencies("A")).contains("B", "C");
    }

    @Test
    void getTransitiveDependencies_emptyForLeaf() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        graph.addNode("A", a);
        assertThat(graph.getTransitiveDependencies("A")).isEmpty();
    }

    @Test
    void topologicalSort_singleNode() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        graph.addNode("A", a);
        assertThat(graph.topologicalSort()).containsExactly("A");
    }

    @Test
    void duplicateAddNode_doesNotOverwrite() {
        PluginDescriptor a = PluginDescriptor.of("A", "1.0", "com.example.A");
        PluginDescriptor a2 = PluginDescriptor.of("A", "2.0", "com.example.A2");
        graph.addNode("A", a);
        graph.addNode("A", a2);
        assertThat(graph.getNodes().get("A").descriptor.version()).isEqualTo("1.0");
    }
}
