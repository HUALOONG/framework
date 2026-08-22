/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.datapermission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * {@link DataPermission} 注解与 {@link DataPermissionContext} 契约测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class DataPermissionContextTest {

    @Test
    void contextDefaultIsClean() {
        assertNull(DataPermissionContext.getCurrentUser());
    }

    @Test
    void setCurrentUserAndGetUser() {
        UserInfo user = UserInfo.of(null, null, null, DataScope.DEPT);
        DataPermissionContext.runWith(user, () -> {
            UserInfo current = DataPermissionContext.getCurrentUser();
            assertNotNull(current);
            assertEquals(DataScope.DEPT, current.dataScope());
        });
        assertNull(DataPermissionContext.getCurrentUser());
    }

    @Test
    void clearSetsNull() {
        DataPermissionContext.runWith(UserInfo.of(1L, null, null, DataScope.ALL), () -> {
            // 在作用域内设置用户
            assertNotNull(DataPermissionContext.getCurrentUser());
        });
        // 作用域结束后清理
        assertNull(DataPermissionContext.getCurrentUser());
    }

    @Test
    void annotationHasCorrectFields() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("listUsers");
        assertTrue(method.isAnnotationPresent(DataPermission.class));
        DataPermission dp = method.getAnnotation(DataPermission.class);
        assertEquals("t_user", dp.tableName());
        assertEquals(DataPermissionColumns.DEPT_COLUMN, dp.deptColumn());
        assertEquals(DataPermissionColumns.USER_COLUMN, dp.userColumn());
    }

    // ---- helper service ----
    static class TestService {
        @DataPermission(tableName = "t_user")
        public void listUsers() {}
    }
}
