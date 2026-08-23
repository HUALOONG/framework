package cn.jowen.framework.plugin.descriptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginVersion} 测试。
 */
class PluginVersionTest {

    @Test
    void parse_majorMinorPatch() {
        PluginVersion v = PluginVersion.parse("1.2.3");
        assertThat(v.getMajor()).isEqualTo(1);
        assertThat(v.getMinor()).isEqualTo(2);
        assertThat(v.getPatch()).isEqualTo(3);
        assertThat(v.getPreRelease()).isNull();
    }

    @Test
    void parse_majorMinor_only_patchDefaultsToZero() {
        PluginVersion v = PluginVersion.parse("2.0");
        assertThat(v.getMajor()).isEqualTo(2);
        assertThat(v.getMinor()).isEqualTo(0);
        assertThat(v.getPatch()).isEqualTo(0);
    }

    @Test
    void parse_withPreRelease() {
        PluginVersion v = PluginVersion.parse("1.0.0-alpha");
        assertThat(v.getMajor()).isEqualTo(1);
        assertThat(v.getMinor()).isEqualTo(0);
        assertThat(v.getPatch()).isEqualTo(0);
        assertThat(v.getPreRelease()).isEqualTo("alpha");
    }

    @Test
    void parse_withBuildMetadata_ignored() {
        PluginVersion v = PluginVersion.parse("1.0.0+build123");
        assertThat(v.getPreRelease()).isNull();
        assertThat(v.getMajor()).isEqualTo(1);
        assertThat(v.getMinor()).isEqualTo(0);
        assertThat(v.getPatch()).isEqualTo(0);
    }

    @Test
    void parse_withPreReleaseAndBuild() {
        PluginVersion v = PluginVersion.parse("1.0.0-rc.1+build.456");
        assertThat(v.getPreRelease()).isEqualTo("rc.1");
        assertThat(v.getMajor()).isEqualTo(1);
        assertThat(v.toString()).isEqualTo("1.0.0-rc.1");
    }

    @Test
    void parse_nullThrowsIllegalArgument() {
        assertThatThrownBy(() -> PluginVersion.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_blankStringThrowsIllegalArgument() {
        assertThatThrownBy(() -> PluginVersion.parse("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_invalidFormatThrowsIllegalArgument() {
        assertThatThrownBy(() -> PluginVersion.parse("1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_nonNumericThrowsIllegalArgument() {
        assertThatThrownBy(() -> PluginVersion.parse("abc.def.ghi"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compareTo_equalVersions_returnsZero() {
        PluginVersion v1 = PluginVersion.parse("1.2.3");
        PluginVersion v2 = PluginVersion.parse("1.2.3");
        assertThat(v1.compareTo(v2)).isEqualTo(0);
    }

    @Test
    void compareTo_majorDiff_firstIsLesser() {
        PluginVersion v1 = PluginVersion.parse("1.0.0");
        PluginVersion v2 = PluginVersion.parse("2.0.0");
        assertThat(v1.compareTo(v2)).isEqualTo(-1);
    }

    @Test
    void compareTo_minorDiff_firstIsLesser() {
        PluginVersion v1 = PluginVersion.parse("1.0.0");
        PluginVersion v2 = PluginVersion.parse("1.1.0");
        assertThat(v1.compareTo(v2)).isEqualTo(-1);
    }

    @Test
    void compareTo_patchDiff_firstIsLesser() {
        PluginVersion v1 = PluginVersion.parse("1.0.0");
        PluginVersion v2 = PluginVersion.parse("1.0.1");
        assertThat(v1.compareTo(v2)).isEqualTo(-1);
    }

    @Test
    void compareTo_preReleaseLessThanRelease() {
        PluginVersion pre = PluginVersion.parse("1.0.0-alpha");
        PluginVersion release = PluginVersion.parse("1.0.0");
        assertThat(pre.compareTo(release)).isEqualTo(-1);
    }

    @Test
    void compareTo_releaseGreaterThanPreRelease() {
        PluginVersion release = PluginVersion.parse("1.0.0");
        PluginVersion pre = PluginVersion.parse("1.0.0-alpha");
        assertThat(release.compareTo(pre)).isEqualTo(1);
    }

    @Test
    void compareTo_null_returnsOne() {
        PluginVersion v = PluginVersion.parse("1.0.0");
        assertThat(v.compareTo(null)).isEqualTo(1);
    }

    @Test
    void isCompatible_withinRange_returnsTrue() {
        PluginVersion v = PluginVersion.parse("1.5.0");
        VersionRange range = new VersionRange("[1.0.0,2.0.0)");
        assertThat(v.isCompatible(range)).isTrue();
    }

    @Test
    void isCompatible_outsideRange_returnsFalse() {
        PluginVersion v = PluginVersion.parse("3.0.0");
        VersionRange range = new VersionRange("[1.0.0,2.0.0)");
        assertThat(v.isCompatible(range)).isFalse();
    }

    @Test
    void toString_fullVersion() {
        PluginVersion v = PluginVersion.parse("1.2.3");
        assertThat(v.toString()).isEqualTo("1.2.3");
    }

    @Test
    void toString_withPreRelease() {
        PluginVersion v = PluginVersion.parse("1.0.0-beta");
        assertThat(v.toString()).isEqualTo("1.0.0-beta");
    }

    @Test
    void parse_withTrimmedWhitespace() {
        PluginVersion v = PluginVersion.parse("  1.2.3  ");
        assertThat(v.getMajor()).isEqualTo(1);
    }
}
