package cn.jowen.framework.extras.common.geo;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * IP 属地解析结果：对应 ip2region 返回的 {@code 国家|区域|省份|城市|ISP} 五段结构。
 *
 * <p>解析容错：原始串为空或段数不足时，缺失段以空串补齐而非抛异常——
 * 属地信息本质是展示性数据，不应因数据缺口导致查询链路失败。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class IpRegionInfo {

    /** 空结果：全部段为空串。 */
    public static final IpRegionInfo EMPTY =
            new IpRegionInfo("", "", "", "", "");

    /** 段数。 */
    private static final int SEGMENTS = 5;

    /** 国家。 */
    private final String country;

    /** 区域。 */
    private final String region;

    /** 省份。 */
    private final String province;

    /** 城市。 */
    private final String city;

    /** 运营商。 */
    private final String isp;

    private IpRegionInfo(String country, String region, String province, String city, String isp) {
        this.country = country;
        this.region = region;
        this.province = province;
        this.city = city;
        this.isp = isp;
    }

    /**
     * 解析 ip2region 原始属地串。
     *
     * @param raw 形如 {@code 中国|0|广东省|深圳市|电信} 的原始串，可为 {@code null}
     * @return 解析结果，不可为 {@code null}（原始串无效时返回 {@link #EMPTY}）
     */
    public static IpRegionInfo parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return EMPTY;
        }
        String[] parts = raw.split("\\|", -1);
        String[] padded = new String[SEGMENTS];
        for (int i = 0; i < SEGMENTS; i++) {
            String part = i < parts.length ? parts[i] : "";
            // "0" 是 ip2region 对缺失段的占位约定
            padded[i] = ("0".equals(part) || part.isBlank()) ? "" : part;
        }
        return new IpRegionInfo(padded[0], padded[1], padded[2], padded[3], padded[4]);
    }

    /**
     * 按五段直接构造。
     *
     * @param country  国家
     * @param region   区域
     * @param province 省份
     * @param city     城市
     * @param isp      运营商
     * @return 实例，不可为 {@code null}
     */
    public static IpRegionInfo of(String country, String region, String province, String city, String isp) {
        return new IpRegionInfo(
                Objects.requireNonNull(country, "country"),
                Objects.requireNonNull(region, "region"),
                Objects.requireNonNull(province, "province"),
                Objects.requireNonNull(city, "city"),
                Objects.requireNonNull(isp, "isp"));
    }

    /**
     * 国家。
     *
     * @return 国家，缺失为空串
     */
    public String getCountry() {
        return country;
    }

    /**
     * 区域。
     *
     * @return 区域，缺失为空串
     */
    public String getRegion() {
        return region;
    }

    /**
     * 省份。
     *
     * @return 省份，缺失为空串
     */
    public String getProvince() {
        return province;
    }

    /**
     * 城市。
     *
     * @return 城市，缺失为空串
     */
    public String getCity() {
        return city;
    }

    /**
     * 运营商。
     *
     * @return 运营商，缺失为空串
     */
    public String getIsp() {
        return isp;
    }

    /**
     * 是否为空结果（全部段缺失）。
     *
     * @return 全部段为空串时返回 true
     */
    public boolean isEmpty() {
        return country.isEmpty() && region.isEmpty() && province.isEmpty()
                && city.isEmpty() && isp.isEmpty();
    }

    /**
     * 展示串：跳过缺失段，空格连接，如 {@code 中国 广东省 深圳市 电信}。
     *
     * @return 展示串，无任何有效段时为空串
     */
    public String display() {
        StringJoiner joiner = new StringJoiner(" ");
        for (String segment : new String[] {country, region, province, city, isp}) {
            if (!segment.isEmpty()) {
                joiner.add(segment);
            }
        }
        return joiner.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IpRegionInfo that)) {
            return false;
        }
        return country.equals(that.country)
                && region.equals(that.region)
                && province.equals(that.province)
                && city.equals(that.city)
                && isp.equals(that.isp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, region, province, city, isp);
    }

    @Override
    public String toString() {
        return "IpRegionInfo{country='" + country + "', region='" + region
                + "', province='" + province + "', city='" + city + "', isp='" + isp + "'}";
    }
}
