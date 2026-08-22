/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.ip2region;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.List;

/**
 * IP 地域解析服务。
 *
 * <p>提供单个 IP 查询、内网 IP 判断等能力。
 * 根据 {@link Ip2RegionProperties#getLoadType()} 自动选择搜索器实现。
 *
 * @author Jowen
 * @date 2026-08-22
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
     * 根据 IP 地址查询地域信息。
     *
     * @param ip IP 地址字符串
     * @return 地域结果，IP 无效或查询失败时返回 {@code null}
     */
    @Nullable
    public IpRegion resolve(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        return searcher.search(ip.trim());
    }

    /**
     * 判断 IP 是否为内网/私有地址。
     *
     * @param ip IP 地址字符串
     * @return true 表示内网 IP
     */
    public boolean isInternalIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return isPrivateIp(ip.trim());
    }

    /**
     * 判断 IP 是否来自受信地址（内网或 loopback）。
     *
     * @param ip IP 地址
     * @return true 表示是内网或保留地址
     */
    public boolean isTrustedSource(String ip) {
        return ip != null && (isPrivateIp(ip) || "127.0.0.1".equals(ip));
    }

    private static IpRegionSearcher buildSearcher(String dbPath, Ip2RegionProperties.LoadType loadType) {
        return switch (loadType) {
            case MEMORY -> new MemorySearcher(dbPath);
            case INDEX -> new VectorIndexSearcher(dbPath);
            case FILE -> new FileBufferSearcher(dbPath);
        };
    }

    /**
     * 判断 IP 是否为私有/内网地址。
     *
     * <p>支持以下私有地址范围：
     * <ul>
     *   <li>10.0.0.0/8</li>
     *   <li>172.16.0.0/12</li>
     *   <li>192.168.0.0/16</li>
     *   <li>127.0.0.0/8</li>
     *   <li>169.254.0.0/16</li>
     * </ul>
     */
    private static boolean isPrivateIp(String ip) {
        try {
            byte[] addr = InetAddress.getByName(ip).getAddress();
            if (addr.length != 4) {
                return false;
            }
            int first = addr[0] & 0xFF;
            int second = addr[1] & 0xFF;
            // 10.0.0.0/8
            if (first == 10) {
                return true;
            }
            // 172.16.0.0/12
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            // 192.168.0.0/16
            if (first == 192 && second == 168) {
                return true;
            }
            // 127.0.0.0/8
            if (first == 127) {
                return true;
            }
            // 169.254.0.0/16 (link-local)
            if (first == 169 && second == 254) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
