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
import cn.jowen.framework.extras.datapermission.UserInfo;
import cn.jowen.framework.extras.datapermission.DataScope;
import cn.jowen.framework.extras.datapermission.TableInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * 数据权限 AOP 切面。
 *
 * <p>环绕 {@link DataPermission} 注解方法，在方法执行前将当前用户注入
 * {@link DataPermissionContext}，方法执行后（finally 块）清理上下文。
 *
 * <p>当前用户从 {@link #resolveCurrentUser()} 解析；本轮简化实现为读取已有上下文，
 * 实际项目应接入 Spring Security 的 {@code Authentication}。
 *
 * @author Jowen
 * @date 2026-08-22
 * @see DataPermission
 * @see DataPermissionContext
 */
@NullMarked
@Aspect
@Component
@ConditionalOnClass(name = {
        "cn.jowen.framework.extras.datapermission.DataPermission",
        "cn.jowen.framework.extras.datapermission.DataPermissionContext"
})
public class DataPermissionAspect {

    @Around("@annotation(dp)")
    public Object around(ProceedingJoinPoint pjp, DataPermission dp) throws Throwable {
        // 白名单校验 TableInfo（会抛出 IllegalArgumentException 防止非法列名）
        TableInfo tableInfo = TableInfo.of(dp.tableName(), dp.deptColumn(), dp.userColumn());

        UserInfo currentUser = resolveCurrentUser();
        if (currentUser != null) {
            DataPermissionContext.setCurrentUser(currentUser);
        }
        try {
            return pjp.proceed();
        } finally {
            // 清理上下文（THREAD_LOCAL 模式必须显式清除）
            DataPermissionContext.setCurrentUser(null);
        }
    }

    /**
     * 解析当前用户。
     *
     * <p>本轮简化：从 {@link DataPermissionContext} 读取（若已由其他拦截器注入）。
     * 实际项目中应从 Spring Security 的 {@code SecurityContextHolder} 获取。
     *
     * @return 当前用户，未找到返回 {@code null}
     */
    private @Nullable UserInfo resolveCurrentUser() {
        UserInfo current = DataPermissionContext.getCurrentUser();
        if (current != null) {
            return current;
        }
        // fallback：从 ThreadLocal 中读取（如有自定义 ContextCarrier 实现）
        // 此处留空，等待业务层注入
        return null;
    }
}
