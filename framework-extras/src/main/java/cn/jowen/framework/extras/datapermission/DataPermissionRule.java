/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.datapermission;

import org.jspecify.annotations.NullMarked;

/**
 * 数据权限规则接口：将用户与表信息转换为参数化 SQL 片段。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public interface DataPermissionRule {

    /**
     * 计算数据权限表达式。
     *
     * @param user  当前用户（不可为 null）
     * @param table 表信息（不可为 null）
     * @return 参数化 SQL 片段与参数列表
     */
    DataPermissionExpression getExpression(UserInfo user, TableInfo table);
}
