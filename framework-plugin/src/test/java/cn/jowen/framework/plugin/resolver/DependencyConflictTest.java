package cn.jowen.framework.plugin.resolver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyConflictTest {

    @Test
    void fields() {
        DependencyConflict conflict = new DependencyConflict("B", "A", List.of("[1.0,2.0]"), false);
        assertThat(conflict.pluginId()).isEqualTo("B");
        assertThat(conflict.requiredBy()).isEqualTo("A");
        assertThat(conflict.requiredVersions()).containsExactly("[1.0,2.0]");
        assertThat(conflict.resolved()).isFalse();
    }

    @Test
    void resolved_true() {
        DependencyConflict conflict = new DependencyConflict("B", "A", List.of("[1.0,2.0]"), true);
        assertThat(conflict.resolved()).isTrue();
    }
}
