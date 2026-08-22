/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.ip2region;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * IP2Region 配置属性载体。
 *
 * <p>通过 {@code framework.extras.ip2region.*} 前缀绑定。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class Ip2RegionProperties {

    /** 是否启用 IP2Region（默认 true）。 */
    private boolean enabled = true;

    /** .xdb 数据库文件路径，相对于 classpath（默认 ip2region.xdb）。 */
    private String dbPath = "ip2region.xdb";

    /** 加载策略：FILE / MEMORY / INDEX（默认 MEMORY）。 */
    private LoadType loadType = LoadType.MEMORY;

    /** 从代理头获取真实 IP 时信任的请求头列表（默认含 X-Forwarded-For、X-Real-IP）。 */
    private List<String> trustedHeaders = List.of("X-Forwarded-For", "X-Real-IP");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public LoadType getLoadType() {
        return loadType;
    }

    public void setLoadType(LoadType loadType) {
        this.loadType = loadType;
    }

    public List<String> getTrustedHeaders() {
        return trustedHeaders;
    }

    public void setTrustedHeaders(List<String> trustedHeaders) {
        this.trustedHeaders = trustedHeaders == null ? List.of() : trustedHeaders;
    }

    /**
     * IP2Region 数据库加载策略枚举。
     */
    public enum LoadType {
        /** 文件加载：每次请求读取文件（最慢，内存占用最小）。 */
        FILE,
        /** 内存加载：将 xdb 文件全量加载到内存（最快，内存开销约 10MB）。 */
        MEMORY,
        /** 向量索引加载：加载向量索引到内存（速度介于 FILE 与 MEMORY 之间）。 */
        INDEX
    }
}
