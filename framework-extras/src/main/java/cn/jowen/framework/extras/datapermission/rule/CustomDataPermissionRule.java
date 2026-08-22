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
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * 自定义数据权限规则：在 {@link DataScope#CUSTOM} 范围下返回预置的 SQL 片段与参数；
 * 非自定义范围不约束。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class CustomDataPermissionRule implements DataPermissionRule {

    private final String customSql;
    private final List<Object> customParams;

    public CustomDataPermissionRule(String customSql, List<Object> customParams) {
        this.customSql = Objects.requireNonNull(customSql, "customSql must not be null");
        this.customParams = customParams == null ? List.of() : List.copyOf(customParams);
    }

    @Override
    public DataPermissionExpression getExpression(UserInfo user, TableInfo table) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(table, "table must not be null");
        if (user.dataScope() == DataScope.CUSTOM) {
            return new DataPermissionExpression(customSql, customParams);
        }
        return DataPermissionExpression.empty();
    }
}
