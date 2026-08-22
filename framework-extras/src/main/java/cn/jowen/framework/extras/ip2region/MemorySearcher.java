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
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MEMORY 加载策略实现。
 *
 * <p>将 .xdb 文件全量加载到堆内存，查询速度最快（通常 < 1ms），适合大多数场景。
 * 内存开销约 10MB。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class MemorySearcher implements IpRegionSearcher {

    private static final Logger log = LoggerFactory.getLogger(MemorySearcher.class);

    private final Searcher searcher;

    /**
     * 从 classpath 或文件系统加载 .xdb 文件到内存。
     *
     * @param dbPath .xdb 数据库文件路径（支持 classpath: 前缀）
     */
    public MemorySearcher(String dbPath) {
        byte[] data = loadBytes(dbPath);
        try {
            this.searcher = Searcher.newWithBuffer(data);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create memory searcher from db: " + dbPath, e);
        }
        log.info("MemorySearcher initialized, dbPath={}, size={}", dbPath, data.length);
    }

    @Override
    @Nullable
    public IpRegion search(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            String result = searcher.search(ip);
            return parseResult(ip, result);
        } catch (Exception e) {
            log.warn("search failed for ip={}", ip, e);
            return null;
        }
    }

    @Override
    public void close() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private static byte[] loadBytes(String dbPath) {
        // 优先尝试从 classpath 加载
        String cleanPath = dbPath.startsWith("classpath:") ? dbPath.substring("classpath:".length()) : dbPath;
        InputStream is = MemorySearcher.class.getClassLoader().getResourceAsStream(cleanPath);
        if (is != null) {
            try (InputStream stream = is) {
                return stream.readAllBytes();
            } catch (Exception e) {
                throw new IllegalStateException("failed to read classpath resource: " + cleanPath, e);
            }
        }
        // 再从文件系统加载
        Path path = Path.of(dbPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("db file not found: " + dbPath);
        }
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read db file: " + dbPath, e);
        }
    }

    private static IpRegion parseResult(String ip, String raw) {
        if (raw == null || raw.isBlank()) {
            return new IpRegion(ip, null, null, null, null, null, "");
        }
        // ip2region 格式：country|region|province|city|isp
        String[] parts = raw.split("\\|", -1);
        String country = safe(parts, 0);
        String region = safe(parts, 1);
        String province = safe(parts, 2);
        String city = safe(parts, 3);
        String isp = safe(parts, 4);
        String fullRegion = String.join(" ", java.util.List.of(region, province, city).stream()
                .filter(s -> s != null && !"0".equals(s) && !s.isBlank())
                .toArray(String[]::new)).trim();
        return new IpRegion(ip, country, region, province, city, isp, fullRegion);
    }

    private static String safe(String[] arr, int index) {
        return (arr != null && index < arr.length && arr[index] != null && !"0".equals(arr[index]))
                ? arr[index] : null;
    }
}
