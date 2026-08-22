/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.datapermission;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 当前用户信息（数据权限计算输入）。
 *
 * @param userId  用户 ID（可为 null）
 * @param deptId  部门 ID（可为 null）
 * @param deptIds 部门及子部门 ID 集合（可为 null，默认空）
 * @param dataScope 数据权限范围（不可为 null）
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public record UserInfo(@Nullable Long userId, @Nullable Long deptId,
        @Nullable List<Long> deptIds, DataScope dataScope) {

    public UserInfo {
        Objects.requireNonNull(dataScope, "dataScope must not be null");
        deptIds = deptIds == null ? List.of() : List.copyOf(deptIds);
    }

    /** 便捷工厂。 */
    public static UserInfo of(@Nullable Long userId, @Nullable Long deptId,
            @Nullable List<Long> deptIds, DataScope dataScope) {
        return new UserInfo(userId, deptId, deptIds, dataScope);
    }
}
