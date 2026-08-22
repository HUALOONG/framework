/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.datapermission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

/**
 * 数据权限注解（声明式，零依赖）。
 *
 * <p>标注在需要注入数据权限上下文的方法上，由 {@link DataPermissionAspect} 在方法执行前
 * 将当前用户写入 {@link DataPermissionContext}，方法执行后清理。
 *
 * <p><b>注意</b>：此注解仅负责上下文注入；实际的 MyBatis SQL 改写不在本框架范围内。
 *
 * <pre>{@code
 * @DataPermission(tableName = "t_user", deptColumn = DataPermissionColumns.DEPT_COLUMN)
 * public List<UserVO> listUsers() { ... }
 * }</pre>
 *
 * @author Jowen
 * @date 2026-08-22
 * @see DataPermissionAspect
 * @see DataPermissionContext
 * @see DataPermissionColumns
 */
@NullMarked
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 表名（全限定或短名，由 {@link TableInfo} 白名单校验）。
     */
    String tableName();

    /**
     * 部门列名，缺省 {@link DataPermissionColumns#DEPT_COLUMN}。
     */
    String deptColumn() default DataPermissionColumns.DEPT_COLUMN;

    /**
     * 用户列名，缺省 {@link DataPermissionColumns#USER_COLUMN}。
     */
    String userColumn() default DataPermissionColumns.USER_COLUMN;
}
