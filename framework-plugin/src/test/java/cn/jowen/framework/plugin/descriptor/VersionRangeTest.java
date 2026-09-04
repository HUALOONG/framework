package cn.jowen.framework.plugin.descriptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link VersionRange} 测试。
 */
class VersionRangeTest {

    @Test
    void constructor_closesBoth() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(range.getLower()).isEqualTo("1.0.0");
        assertThat(range.getUpper()).isEqualTo("2.0.0");
        assertThat(range.isLowerInclusive()).isTrue();
        assertThat(range.isUpperInclusive()).isTrue();
    }

    @Test
    void constructor_openLower_closedUpper() {
        VersionRange range = new VersionRange("(1.0.0,2.0.0]");
        assertThat(range.isLowerInclusive()).isFalse();
        assertThat(range.isUpperInclusive()).isTrue();
    }

    @Test
    void constructor_closedLower_openUpper() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0)");
        assertThat(range.isLowerInclusive()).isTrue();
        assertThat(range.isUpperInclusive()).isFalse();
    }

    @Test
    void constructor_openBoth() {
        VersionRange range = new VersionRange("(1.0.0,2.0.0)");
        assertThat(range.isLowerInclusive()).isFalse();
        assertThat(range.isUpperInclusive()).isFalse();
    }

    @Test
    void contains_versionWithinInclusiveRange() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(range.contains("1.5.0")).isTrue();
    }

    @Test
    void contains_versionAtLowerInclusiveBoundary() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(range.contains("1.0.0")).isTrue();
    }

    @Test
    void contains_versionAtUpperInclusiveBoundary() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(range.contains("2.0.0")).isTrue();
    }

    @Test
    void contains_versionBelowLowerBoundary_returnsFalse() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(range.contains("0.9.9")).isFalse();
    }

    @Test
    void contains_versionAboveUpperBoundary_returnsFalse() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(range.contains("2.0.1")).isFalse();
    }

    @Test
    void contains_versionAtLowerExclusiveBoundary_returnsFalse() {
        VersionRange range = new VersionRange("(1.0.0,2.0.0]");
        assertThat(range.contains("1.0.0")).isFalse();
    }

    @Test
    void contains_versionAtUpperExclusiveBoundary_returnsFalse() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0)");
        assertThat(range.contains("2.0.0")).isFalse();
    }

    @Test
    void contains_withPluginVersion() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        PluginVersion v = PluginVersion.parse("1.5.0");
        assertThat(range.contains(v)).isTrue();
    }

    @Test
    void intersect_overlappingRanges() {
        VersionRange r1 = new VersionRange("[1.0.0,3.0.0]");
        VersionRange r2 = new VersionRange("[2.0.0,4.0.0]");
        VersionRange intersection = r1.intersect(r2);
        assertThat(intersection).isNotNull();
        assertThat(intersection.getLower()).isEqualTo("2.0.0");
        assertThat(intersection.getUpper()).isEqualTo("3.0.0");
    }

    @Test
    void intersect_nonOverlapping_returnsNull() {
        VersionRange r1 = new VersionRange("[1.0.0,2.0.0]");
        VersionRange r2 = new VersionRange("[3.0.0,4.0.0]");
        assertThat(r1.intersect(r2)).isNull();
    }

    @Test
    void intersect_partiallyOpen() {
        VersionRange r1 = new VersionRange("[1.0.0,3.0.0)");
        VersionRange r2 = new VersionRange("(2.0.0,4.0.0]");
        VersionRange intersection = r1.intersect(r2);
        assertThat(intersection).isNotNull();
    }

    @Test
    void toString_closedClosed() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(range.toString()).isEqualTo("[1.0.0,2.0.0]");
    }

    @Test
    void toString_openOpen() {
        VersionRange range = new VersionRange("(1.0.0,2.0.0)");
        assertThat(range.toString()).isEqualTo("(1.0.0,2.0.0)");
    }

    @Test
    void constructor_nullExpression_throwsException() {
        assertThatThrownBy(() -> new VersionRange(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_blankExpression_throwsException() {
        assertThatThrownBy(() -> new VersionRange("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_missingComma_throwsException() {
        assertThatThrownBy(() -> new VersionRange("[1.0.0,2.0.0]".replace(",", ";")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_tooShortExpression_throwsException() {
        assertThatThrownBy(() -> new VersionRange("[1]"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contains_twoPartVersions() {
        VersionRange range = new VersionRange("[1.0,2.0]");
        assertThat(range.contains("1.5")).isTrue();
    }

    @Test
    void intersect_thisLowerGreater_usesThisLower() {
        VersionRange r1 = new VersionRange("[2.0.0,5.0.0]");
        VersionRange r2 = new VersionRange("[1.0.0,4.0.0]");
        VersionRange intersection = r1.intersect(r2);
        assertThat(intersection).isNotNull();
        assertThat(intersection.getLower()).isEqualTo("2.0.0");
    }

    @Test
    void intersect_equalLower_andsInclusivity() {
        VersionRange r1 = new VersionRange("[1.0.0,5.0.0]");
        VersionRange r2 = new VersionRange("[1.0.0,4.0.0]");
        VersionRange intersection = r1.intersect(r2);
        assertThat(intersection).isNotNull();
        assertThat(intersection.getLower()).isEqualTo("1.0.0");
        assertThat(intersection.isLowerInclusive()).isTrue();
    }

    @Test
    void intersect_thisUpperGreater_usesStricterUpper() {
        VersionRange r1 = new VersionRange("[1.0.0,4.0.0]");
        VersionRange r2 = new VersionRange("[2.0.0,3.0.0]");
        VersionRange intersection = r1.intersect(r2);
        assertThat(intersection).isNotNull();
        // 交集上界取更严格的较小值（other.upper）
        assertThat(intersection.getUpper()).isEqualTo("3.0.0");
    }

    @Test
    void intersect_equalUpper_andsInclusivity() {
        VersionRange r1 = new VersionRange("[1.0.0,3.0.0]");
        VersionRange r2 = new VersionRange("[2.0.0,3.0.0]");
        VersionRange intersection = r1.intersect(r2);
        assertThat(intersection).isNotNull();
        assertThat(intersection.getUpper()).isEqualTo("3.0.0");
        assertThat(intersection.isUpperInclusive()).isTrue();
    }
}
