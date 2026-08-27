package cn.jowen.framework.plugin.resolver;

import cn.jowen.framework.plugin.descriptor.VersionRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VersionArbitratorTest {

    @Test
    void intersect_twoOverlappingRanges() {
        List<VersionRange> ranges = List.of(
                new VersionRange("[1.0.0,3.0.0]"),
                new VersionRange("[2.0.0,4.0.0]")
        );
        VersionRange result = VersionArbitrator.intersect(ranges);
        assertThat(result).isNotNull();
        assertThat(result.getLower()).isEqualTo("2.0.0");
        assertThat(result.getUpper()).isEqualTo("3.0.0");
    }

    @Test
    void intersect_nonOverlapping_returnsNull() {
        List<VersionRange> ranges = List.of(
                new VersionRange("[1.0.0,2.0.0]"),
                new VersionRange("[3.0.0,4.0.0]")
        );
        assertThat(VersionArbitrator.intersect(ranges)).isNull();
    }

    @Test
    void intersect_singleRange_returnsSame() {
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(VersionArbitrator.intersect(List.of(range))).isEqualTo(range);
    }

    @Test
    void intersect_empty_returnsNull() {
        assertThat(VersionArbitrator.intersect(List.of())).isNull();
    }

    @Test
    void intersect_threeRanges() {
        List<VersionRange> ranges = List.of(
                new VersionRange("[1.0.0,5.0.0]"),
                new VersionRange("[2.0.0,4.0.0]"),
                new VersionRange("[3.0.0,6.0.0]")
        );
        VersionRange result = VersionArbitrator.intersect(ranges);
        assertThat(result).isNotNull();
        assertThat(result.getLower()).isEqualTo("3.0.0");
        assertThat(result.getUpper()).isEqualTo("4.0.0");
    }

    @Test
    void resolveHighest_returnsHighestCompatible() {
        List<String> versions = List.of("1.0.0", "1.5.0", "1.8.0", "2.0.0");
        VersionRange range = new VersionRange("[1.0.0,2.0.0)");
        assertThat(VersionArbitrator.resolveHighest(versions, range)).isEqualTo("1.8.0");
    }

    @Test
    void resolveHighest_noCompatible_returnsNull() {
        List<String> versions = List.of("1.0.0", "1.1.0");
        VersionRange range = new VersionRange("[2.0.0,3.0.0]");
        assertThat(VersionArbitrator.resolveHighest(versions, range)).isNull();
    }

    @Test
    void resolveHighest_singleCompatible() {
        List<String> versions = List.of("1.0.0");
        VersionRange range = new VersionRange("[1.0.0,2.0.0]");
        assertThat(VersionArbitrator.resolveHighest(versions, range)).isEqualTo("1.0.0");
    }

    @Test
    void intersect_partialOverlap() {
        List<VersionRange> ranges = List.of(
                new VersionRange("[1.0.0,2.0.0)"),
                new VersionRange("(1.5.0,3.0.0]")
        );
        VersionRange result = VersionArbitrator.intersect(ranges);
        assertThat(result).isNotNull();
    }
}
