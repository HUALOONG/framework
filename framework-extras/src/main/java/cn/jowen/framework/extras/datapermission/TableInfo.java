/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.datapermission;

import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 表信息（规则输入）：表名 + 部门列 + 用户列。
 *
 * <p>构造时对 {@code tableName} 做标识符校验，对 {@code deptColumn}/{@code userColumn} 做白名单校验
 * （必须等于 {@link DataPermissionColumns} 中的已知常量），不满足即抛 {@link IllegalArgumentException}，
 * 以防范 SQL 注入。
 *
 * @param tableName   表名（可带 schema 点号，如 {@code t_user} / {@code sys.t_user}）
 * @param deptColumn  部门列名（白名单内或 null）
 * @param userColumn  用户列名（白名单内或 null）
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public record TableInfo(String tableName, @Nullable String deptColumn, @Nullable String userColumn) {

    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*\\.)?[A-Za-z_][A-Za-z0-9_]*$");

    public TableInfo {
        Objects.requireNonNull(tableName, "tableName must not be null");
        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法表名（疑似注入）: " + tableName);
        }
        requireWhitelistedColumn(deptColumn);
        requireWhitelistedColumn(userColumn);
    }

    /** 仅指定表名，部门/用户列取默认常量。 */
    public static TableInfo of(String tableName) {
        return new TableInfo(tableName, DataPermissionColumns.DEPT_COLUMN, DataPermissionColumns.USER_COLUMN);
    }

    /** 指定表名与部门列，用户列取默认常量。 */
    public static TableInfo of(String tableName, @Nullable String deptColumn) {
        return new TableInfo(tableName, deptColumn, DataPermissionColumns.USER_COLUMN);
    }

    /** 指定表名、部门列与用户列。 */
    public static TableInfo of(String tableName, @Nullable String deptColumn, @Nullable String userColumn) {
        return new TableInfo(tableName, deptColumn, userColumn);
    }

    private static void requireWhitelistedColumn(@Nullable String column) {
        if (column == null) {
            return;
        }
        if (!DataPermissionColumns.DEPT_COLUMN.equals(column) && !DataPermissionColumns.USER_COLUMN.equals(column)) {
            throw new IllegalArgumentException("非法列名（不在白名单）: " + column);
        }
    }
}
