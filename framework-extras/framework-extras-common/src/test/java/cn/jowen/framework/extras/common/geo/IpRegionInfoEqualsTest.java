package cn.jowen.framework.extras.common.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link IpRegionInfo} 值相等契约测试：属地信息常用于缓存键与去重，equals/hashCode 一致性是硬前提。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class IpRegionInfoEqualsTest {

    private static final String RAW = "中国|0|广东省|深圳市|电信";

    @Test
    void equals_sameValues_returnsTrueAndHashCodeMatches() {
        IpRegionInfo a = IpRegionInfo.parse(RAW);
        IpRegionInfo b = IpRegionInfo.parse(RAW);

        assertThat(a.equals(b)).isTrue();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_identicalInstance_returnsTrue() {
        IpRegionInfo a = IpRegionInfo.parse(RAW);

        assertThat(a.equals(a)).isTrue();
    }

    @Test
    void equals_differingSegment_returnsFalse() {
        IpRegionInfo a = IpRegionInfo.parse("中国|0|广东省|深圳市|电信");
        IpRegionInfo b = IpRegionInfo.parse("中国|0|北京市|北京市|联通");

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void equals_differentType_returnsFalse() {
        IpRegionInfo a = IpRegionInfo.parse(RAW);

        assertThat(a.equals("中国|0|广东省|深圳市|电信")).isFalse();
        assertThat(a.equals(null)).isFalse();
    }
}
