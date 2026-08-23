package cn.jowen.framework.extras.ip2region;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * IP 地域解析结果记录。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record IpRegion(
        /* 被查询的 IP 地址。 */
        String ip,
        /* 国家，如 "中国"。 */
        @Nullable String country,
        /* 区域，如 "东亚"。 */
        @Nullable String region,
        /* 省份，如 "广东省"。 */
        @Nullable String province,
        /* 城市，如 "深圳市"。 */
        @Nullable String city,
        /* ISP，如 "电信"。 */
        @Nullable String isp,
        /* 完整地域描述，由 region + province + city 拼接而成。 */
        String fullRegion
) {

    /**
     * 是否为内网/私有 IP。
     */
    public boolean isInternalIp() {
        return "内网IP".equals(country);
    }
}
