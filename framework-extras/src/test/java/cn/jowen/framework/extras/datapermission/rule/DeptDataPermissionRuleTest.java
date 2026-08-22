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
import cn.jowen.framework.extras.datapermission.DataPermissionColumns;
import cn.jowen.framework.extras.datapermission.TableInfo;
import cn.jowen.framework.extras.datapermission.UserInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeptDataPermissionRuleTest {

    private final DataPermissionRule rule = new DeptDataPermissionRule();

    @Test
    void deptAndChildProducesInClauseWithAllIds() {
        UserInfo user = UserInfo.of(1L, 10L, List.of(10L, 11L, 12L), DataScope.DEPT_AND_CHILD);
        TableInfo table = TableInfo.of("t_user", DataPermissionColumns.DEPT_COLUMN);

        DataPermissionExpression expr = rule.getExpression(user, table);

        assertThat(expr.sql()).isEqualTo("t_user.dept_id IN (?, ?, ?)");
        assertThat(expr.params()).containsExactly(10L, 11L, 12L);
    }

    @Test
    void deptScopeUsesOnlyOwnDept() {
        UserInfo user = UserInfo.of(1L, 10L, List.of(), DataScope.DEPT);
        TableInfo table = TableInfo.of("t_user", DataPermissionColumns.DEPT_COLUMN);

        DataPermissionExpression expr = rule.getExpression(user, table);

        assertThat(expr.sql()).isEqualTo("t_user.dept_id = ?");
        assertThat(expr.params()).containsExactly(10L);
    }

    @Test
    void selfScopeUsesUserColumn() {
        UserInfo user = UserInfo.of(1L, 10L, List.of(), DataScope.SELF);
        TableInfo table = TableInfo.of("t_user");

        DataPermissionExpression expr = rule.getExpression(user, table);

        assertThat(expr.sql()).isEqualTo("t_user.create_by = ?");
        assertThat(expr.params()).containsExactly(1L);
    }

    @Test
    void allScopeReturnsEmptyExpression() {
        UserInfo user = UserInfo.of(1L, 10L, List.of(), DataScope.ALL);
        TableInfo table = TableInfo.of("t_user");

        DataPermissionExpression expr = rule.getExpression(user, table);

        assertThat(expr.sql()).isEmpty();
        assertThat(expr.params()).isEmpty();
    }

    @Test
    void rejectsIllegalColumnName() {
        assertThatThrownBy(() -> TableInfo.of("t_user", "evil_column"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsIllegalTableName() {
        assertThatThrownBy(() -> TableInfo.of("t_user; DROP TABLE users"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
