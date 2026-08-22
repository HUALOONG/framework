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

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * INDEX（向量索引）加载策略实现。
 *
 * <p>加载向量索引到内存，数据库文件保持在磁盘。查询速度介于 FILE 与 MEMORY 之间。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class VectorIndexSearcher implements IpRegionSearcher {

    private final Searcher searcher;

    public VectorIndexSearcher(String dbPath) {
        try (RandomAccessFile raf = new RandomAccessFile(dbPath, "r")) {
            byte[] vectorIndex = Searcher.loadVectorIndex(raf);
            this.searcher = Searcher.newWithVectorIndex(dbPath, vectorIndex);
        } catch (IOException e) {
            throw new IllegalStateException("failed to create vector index searcher from: " + dbPath, e);
        }
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

    private static IpRegion parseResult(String ip, String raw) {
        if (raw == null || raw.isBlank()) {
            return new IpRegion(ip, null, null, null, null, null, "");
        }
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
