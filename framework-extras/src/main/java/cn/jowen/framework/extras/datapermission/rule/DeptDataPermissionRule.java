/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.datapermission.rule;

import cn.jowen.framework.extras.datapermission.DataPermissionColumns;
import cn.jowen.framework.extras.datapermission.DataPermissionExpression;
import cn.jowen.framework.extras.datapermission.DataPermissionRule;
import cn.jowen.framework.extras.datapermission.DataScope;
import cn.jowen.framework.extras.datapermission.TableInfo;
import cn.jowen.framework.extras.datapermission.UserInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * 部门数据权限规则：按用户数据范围生成部门/用户列的参数化条件。
 *
 * <p>列名仅来自 {@link DataPermissionColumns} 常量（由 {@link TableInfo} 白名单保证），
 * 值一律参数化占位符，杜绝 SQL 注入。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class DeptDataPermissionRule implements DataPermissionRule {

    @Override
    public DataPermissionExpression getExpression(UserInfo user, TableInfo table) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(table, "table must not be null");
        String deptCol = table.tableName() + "." + table.deptColumn();
        String userCol = table.tableName() + "." + table.userColumn();

        return switch (user.dataScope()) {
            case ALL -> DataPermissionExpression.empty();
            case DEPT_AND_CHILD -> inClause(deptCol, collectDeptIds(user));
            case DEPT -> eqClause(deptCol, user.deptId());
            case SELF -> eqClause(userCol, user.userId());
            case CUSTOM -> DataPermissionExpression.empty();
        };
    }

    private static List<Object> collectDeptIds(UserInfo user) {
        List<Object> ids = new ArrayList<>();
        if (user.deptId() != null) {
            ids.add(user.deptId());
        }
        for (Long deptId : user.deptIds()) {
            if (deptId != null && !ids.contains(deptId)) {
                ids.add(deptId);
            }
        }
        return ids;
    }

    static DataPermissionExpression inClause(String column, List<Object> values) {
        if (values.isEmpty()) {
            return new DataPermissionExpression("(1=0)", List.of());
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(values.size(), "?"));
        return new DataPermissionExpression(column + " IN (" + placeholders + ")", values);
    }

    static DataPermissionExpression eqClause(String column, Object value) {
        return new DataPermissionExpression(column + " = ?", List.of(value));
    }
}
