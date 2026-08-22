/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.datapermission.rule;

import cn.jowen.framework.extras.datapermission.DataPermissionExpression;
import cn.jowen.framework.extras.datapermission.DataPermissionRule;
import cn.jowen.framework.extras.datapermission.DataScope;
import cn.jowen.framework.extras.datapermission.TableInfo;
import cn.jowen.framework.extras.datapermission.UserInfo;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * 用户数据权限规则：仅本人（{@code create_by = ?}）时生成条件；其余范围不约束。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class UserDataPermissionRule implements DataPermissionRule {

    @Override
    public DataPermissionExpression getExpression(UserInfo user, TableInfo table) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(table, "table must not be null");
        if (user.dataScope() == DataScope.SELF) {
            String userCol = table.tableName() + "." + table.userColumn();
            return DeptDataPermissionRule.eqClause(userCol, user.userId());
        }
        return DataPermissionExpression.empty();
    }
}
