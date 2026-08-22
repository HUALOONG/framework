/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.ip2region;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IpRegionService 测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class IpRegionServiceTest {

    private static final Path XDB_FILE = Path.of("src/test/resources/ip2region.xdb");
    private static boolean xdbAvailable;
    private static IpRegionService service;

    static {
        try {
            xdbAvailable = Files.exists(XDB_FILE) && Files.size(XDB_FILE) > 1000;
        } catch (Exception e) {
            xdbAvailable = false;
        }
    }

    @BeforeAll
    static void setUp() {
        Ip2RegionProperties props = new Ip2RegionProperties();
        props.setLoadType(Ip2RegionProperties.LoadType.MEMORY);
        if (xdbAvailable) {
            props.setDbPath(XDB_FILE.toString());
            service = new IpRegionService(props);
        } else {
            // 无 xdb 文件时，service 为 null，只测试不依赖 xdb 的方法
            service = null;
        }
    }

    @Test
    void resolveNullIpReturnsNull() {
        if (service == null) {
            // 无 xdb 时跳过需要 service 的测试
            return;
        }
        assertThat(service.resolve(null)).isNull();
        assertThat(service.resolve("")).isNull();
        assertThat(service.resolve("   ")).isNull();
    }

    @Test
    void isInternalIp() {
        // isInternalIp 基于 Util.isPrivateIP，不依赖 xdb
        if (service == null) {
            // 无法测试，跳过
            org.junit.jupiter.api.Assumptions.abort("ip2region.xdb not found, skipping isInternalIp test");
        }
        assertThat(service.isInternalIp("192.168.1.1")).isTrue();
        assertThat(service.isInternalIp("10.0.0.1")).isTrue();
        assertThat(service.isInternalIp("127.0.0.1")).isTrue();
        assertThat(service.isInternalIp("172.16.0.1")).isTrue();
        assertThat(service.isInternalIp("8.8.8.8")).isFalse();
        assertThat(service.isInternalIp("1.1.1.1")).isFalse();
        assertThat(service.isInternalIp("223.5.5.5")).isFalse();
    }

    @Test
    void privateIpRanges() {
        if (service == null) {
            org.junit.jupiter.api.Assumptions.abort("ip2region.xdb not found, skipping privateIpRanges test");
        }
        assertThat(service.isInternalIp("10.0.0.1")).isTrue();
        assertThat(service.isInternalIp("10.255.255.255")).isTrue();
        assertThat(service.isInternalIp("172.16.0.1")).isTrue();
        assertThat(service.isInternalIp("172.31.255.255")).isTrue();
        assertThat(service.isInternalIp("192.168.0.1")).isTrue();
        assertThat(service.isInternalIp("192.168.255.255")).isTrue();
        assertThat(service.isInternalIp("169.254.1.1")).isTrue();
    }

    /** 需要 xdb 文件的完整功能测试（文件不存在时跳过）。 */
    @Test
    void resolvePublicIpWithXdb() {
        if (!xdbAvailable) {
            org.junit.jupiter.api.Assumptions.abort("ip2region.xdb not found, skipping");
        }
        IpRegion region = service.resolve("8.8.8.8");
        assertThat(region).isNotNull();
        assertThat(region.ip()).isEqualTo("8.8.8.8");
    }
}
