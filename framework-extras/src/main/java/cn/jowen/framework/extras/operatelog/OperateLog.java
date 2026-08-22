/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.operatelog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（声明式，零依赖）；由后续 AOP 拦截器读取并组装 {@link OperateLogRecord}。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OperateLog {

    /** 业务模块（如 "用户管理"）。 */
    String module();

    /** 操作动作（如 "CREATE"）。 */
    String action();

    /** 描述（默认空）。 */
    String description() default "";

    /** 操作内容（默认空）。 */
    String content() default "";

    /** 是否异步分发（默认 true）。 */
    boolean async() default true;

    /** 扩展信息表达式（默认空，留给后续 AOP 解析）。 */
    String extra() default "";
}
