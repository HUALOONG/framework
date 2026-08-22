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

/**
 * IP 地域搜索器接口，封装三种加载策略（FILE / MEMORY / INDEX）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public interface IpRegionSearcher {

    /**
     * 根据 IP 地址查询地域信息。
     *
     * @param ip IP 地址字符串，如 "8.8.8.8"
     * @return 地域结果，查询失败时返回 {@code null}
     */
    @Nullable
    IpRegion search(String ip);

    /**
     * 关闭搜索器，释放底层资源。
     */
    void close();
}
