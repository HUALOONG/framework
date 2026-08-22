package cn.jowen.framework.plugin.dependency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionRangeTest {

    @Test
    void inclusiveRange() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(range.matches("1.0.0")).isTrue();
        assertThat(range.matches("1.5.0")).isTrue();
        assertThat(range.matches("2.0.0")).isTrue();
        assertThat(range.matches("0.9.9")).isFalse();
        assertThat(range.matches("2.0.1")).isFalse();
    }

    @Test
    void exclusiveUpperBound() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0)");
        assertThat(range.matches("2.0.0")).isFalse();
        assertThat(range.matches("1.99.99")).isTrue();
    }

    @Test
    void exclusiveLowerBound() {
        VersionRange range = new VersionRange("(1.0.0,2.0.0)");
        assertThat(range.matches("1.0.0")).isFalse();
        assertThat(range.matches("1.0.1")).isTrue();
    }

    @Test
    void invalidExpressionRejected() {
        assertThatThrownBy(() -> new VersionRange("invalid"))
                .isInstanceOf(DependencyResolutionException.class);
    }

    @Test
    void emptyExpressionRejected() {
        assertThatThrownBy(() -> new VersionRange(""))
                .isInstanceOf(DependencyResolutionException.class);
    }

    @Test
    void compareVersions() {
        assertThat(VersionRange.compareVersions("1.0.0", "2.0.0")).isLessThan(0);
        assertThat(VersionRange.compareVersions("2.0.0", "1.0.0")).isGreaterThan(0);
        assertThat(VersionRange.compareVersions("1.0.0", "1.0.0")).isEqualTo(0);
        assertThat(VersionRange.compareVersions("1.2.3", "1.2.4")).isLessThan(0);
    }
}
