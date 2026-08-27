package cn.jowen.framework.plugin.resolver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResolutionResultTest {

    @Test
    void ok_returnsResolvable() {
        ResolutionResult result = ResolutionResult.ok(List.of("A", "B"));
        assertThat(result.isResolvable()).isTrue();
        assertThat(result.loadOrder()).containsExactly("A", "B");
        assertThat(result.missing()).isEmpty();
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    void failed_withMissing_returnsUnresolvable() {
        ResolutionResult result = ResolutionResult.failed(List.of("A -> B"), List.of());
        assertThat(result.isResolvable()).isFalse();
        assertThat(result.missing()).hasSize(1);
    }

    @Test
    void failed_withConflicts_returnsUnresolvable() {
        DependencyConflict conflict = new DependencyConflict("B", "A", List.of("[1.0,2.0]"), false);
        ResolutionResult result = ResolutionResult.failed(List.of(), List.of(conflict));
        assertThat(result.isResolvable()).isFalse();
        assertThat(result.conflicts()).hasSize(1);
    }

    @Test
    void failed_withBoth_returnsUnresolvable() {
        DependencyConflict conflict = new DependencyConflict("B", "A", List.of("[1.0,2.0]"), false);
        ResolutionResult result = ResolutionResult.failed(List.of("A -> C"), List.of(conflict));
        assertThat(result.isResolvable()).isFalse();
        assertThat(result.missing()).hasSize(1);
        assertThat(result.conflicts()).hasSize(1);
    }
}
