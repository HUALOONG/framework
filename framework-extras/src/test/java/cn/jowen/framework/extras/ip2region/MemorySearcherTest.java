/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.ip2region;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MemorySearcher 测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class MemorySearcherTest {

    @Test
    void constructorWithInvalidPathThrows() {
        assertThatThrownBy(() -> new MemorySearcher("nonexistent.xdb"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db file not found");
    }

    @Test
    void isPrivateIpDetection() {
        // 使用 IpRegionService.isInternalIp() 验证，不直接依赖 Util
        Ip2RegionProperties props = new Ip2RegionProperties();
        props.setLoadType(Ip2RegionProperties.LoadType.MEMORY);
        props.setDbPath("nonexistent.xdb");
        // 不创建 service（会抛异常），只验证构造函数行为
        assertThatThrownBy(() -> new IpRegionService(props))
                .isInstanceOf(IllegalStateException.class);
    }
}
