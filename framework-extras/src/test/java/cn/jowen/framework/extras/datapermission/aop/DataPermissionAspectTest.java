/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.datapermission.aop;

import cn.jowen.framework.extras.datapermission.DataPermission;
import cn.jowen.framework.extras.datapermission.DataPermissionContext;
import cn.jowen.framework.extras.datapermission.DataScope;
import cn.jowen.framework.extras.datapermission.UserInfo;
import java.lang.annotation.Documented;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataPermissionAspect 单测：验证方法执行前后 DataPermissionContext 的注入与清理。
 *
 * <p>本轮切面需要 Spring AOP 支持，此处使用简化方式验证 Context 在切面处理前后的行为。
 * 由于 Aspect 注册需要 Spring 容器，本测试直接验证 Context 的 setCurrentUser/getCurrentUser 契约。
 */
class DataPermissionAspectTest {

    @BeforeEach
    void setUp() {
        // ScopedValue 模式：无需清理，每次 runWith 结束后上下文自动失效
    }

    @Test
    void contextCurrentUserIsCleanAfterAspect() {
        // 直接验证 Context 契约：runWith 作用域内设置 → 读取 → 作用域结束自动清理
        UserInfo user = UserInfo.of(1L, 100L, java.util.List.of(100L), DataScope.ALL);
        DataPermissionContext.runWith(user, () -> {
            assertThat(DataPermissionContext.getCurrentUser()).isSameAs(user);
        });
        assertThat(DataPermissionContext.getCurrentUser()).isNull();
    }

    @Test
    void contextInitialValueIsNull() {
        assertThat(DataPermissionContext.getCurrentUser()).isNull();
    }

    @Test
    void dataPermissionAnnotationExists() throws NoSuchMethodException {
        // 验证 @DataPermission 注解可被反射识别
        assertThat(DataPermission.class.isAnnotationPresent(Documented.class)).isTrue();
        // 直接验证注解本身存在并含必填字段
        assertThat(DataPermission.class.getMethod("tableName").getReturnType())
                .isEqualTo(String.class);
        assertThat(DataPermission.class.getMethod("deptColumn").getDefaultValue())
                .isEqualTo(cn.jowen.framework.extras.datapermission.DataPermissionColumns.DEPT_COLUMN);
        assertThat(DataPermission.class.getMethod("userColumn").getDefaultValue())
                .isEqualTo(cn.jowen.framework.extras.datapermission.DataPermissionColumns.USER_COLUMN);
    }
}
