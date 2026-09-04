package cn.jowen.framework.extras.common.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link IpRegionInfo} 解析测试：覆盖五段完整、缺失补齐、"0" 占位、空值与展示串。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class IpRegionInfoTest {

    @Test
    void parse_fullSegments() {
        IpRegionInfo info = IpRegionInfo.parse("中国|0|广东省|深圳市|电信");

        assertThat(info.getCountry()).isEqualTo("中国");
        assertThat(info.getRegion()).isEmpty();
        assertThat(info.getProvince()).isEqualTo("广东省");
        assertThat(info.getCity()).isEqualTo("深圳市");
        assertThat(info.getIsp()).isEqualTo("电信");
        assertThat(info.isEmpty()).isFalse();
    }

    @Test
    void parse_null_returnsEmpty() {
        assertThat(IpRegionInfo.parse(null)).isEqualTo(IpRegionInfo.EMPTY);
        assertThat(IpRegionInfo.parse(null).isEmpty()).isTrue();
    }

    @Test
    void parse_blank_returnsEmpty() {
        assertThat(IpRegionInfo.parse("")).isEqualTo(IpRegionInfo.EMPTY);
        assertThat(IpRegionInfo.parse("   ")).isEqualTo(IpRegionInfo.EMPTY);
    }

    @Test
    void parse_missingSegments_paddedWithEmpty() {
        IpRegionInfo info = IpRegionInfo.parse("中国|华南|广东省");

        assertThat(info.getCountry()).isEqualTo("中国");
        assertThat(info.getRegion()).isEqualTo("华南");
        assertThat(info.getProvince()).isEqualTo("广东省");
        assertThat(info.getCity()).isEmpty();
        assertThat(info.getIsp()).isEmpty();
    }

    @Test
    void parse_zeroPlaceholders_treatedAsMissing() {
        IpRegionInfo info = IpRegionInfo.parse("中国|0|0|0|0");

        assertThat(info.getCountry()).isEqualTo("中国");
        assertThat(info.getRegion()).isEmpty();
        assertThat(info.getProvince()).isEmpty();
        assertThat(info.getCity()).isEmpty();
        assertThat(info.getIsp()).isEmpty();
    }

    @Test
    void display_skipsEmptySegments() {
        assertThat(IpRegionInfo.parse("中国|0|广东省|深圳市|电信").display())
                .isEqualTo("中国 广东省 深圳市 电信");
        assertThat(IpRegionInfo.parse("中国|0|0|0|0").display()).isEqualTo("中国");
        assertThat(IpRegionInfo.parse("").display()).isEmpty();
    }

    @Test
    void of_equalsHashCode() {
        IpRegionInfo a = IpRegionInfo.of("中国", "", "广东省", "深圳市", "电信");
        IpRegionInfo b = IpRegionInfo.parse("中国|0|广东省|深圳市|电信");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(IpRegionInfo.EMPTY);
        assertThat(a.toString()).contains("广东省");
    }
}
