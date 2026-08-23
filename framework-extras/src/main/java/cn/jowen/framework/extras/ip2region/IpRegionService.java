package cn.jowen.framework.extras.ip2region;

import cn.jowen.framework.extras.config.Ip2RegionProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

/**
 * IP 地域解析服务。
 *
 * <p>提供单个 IP 查询、内网 IP 判断等能力。
 * 根据 {@link Ip2RegionProperties#getLoadType()} 自动选择搜索器实现。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
@Service
@EnableConfigurationProperties(Ip2RegionProperties.class)
public class IpRegionService {

    private static final Logger log = LoggerFactory.getLogger(IpRegionService.class);

    private final Ip2RegionProperties properties;
    private final IpRegionSearcher searcher;

    public IpRegionService(Ip2RegionProperties properties) {
        this.properties = properties;
        this.searcher = buildSearcher(properties.getDbPath(), properties.getLoadType());
    }

    /**
     * 解析单个 IP 的地域信息。
     *
     * @param ip IP 地址字符串
     * @return 地域信息，解析失败时返回 null
     */
    public @Nullable IpRegion resolve(String ip) {
        if (ip.isBlank()) {
            return null;
        }
        try {
            return searcher.search(ip);
        } catch (Exception e) {
            log.warn("IP 地域解析失败: {}", ip, e);
            return null;
        }
    }

    /**
     * 判断是否为内网 IP。
     *
     * @param ip IP 地址字符串
     * @return true 表示内网 IP
     */
    public boolean isInternalIp(String ip) {
        if (ip.isBlank()) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private static IpRegionSearcher buildSearcher(String dbPath, Ip2RegionProperties.LoadType loadType) {
    return IpRegionSearcher.of(dbPath, mapLoadType(loadType));
}

    private static IpRegionSearcher.LoadType mapLoadType(Ip2RegionProperties.LoadType loadType) {
        return switch (loadType) {
            case FILE -> IpRegionSearcher.LoadType.FILE;
            case MEMORY -> IpRegionSearcher.LoadType.MEMORY;
            case INDEX -> IpRegionSearcher.LoadType.INDEX;
        };
    }
}
