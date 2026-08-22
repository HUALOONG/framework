/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Extras 模块总开关注解。
 *
 * <p>在 Spring Boot 启动类上添加此注解，即可开启 framework-extras 全部增强能力
 * （captcha / storage / notification / operatelog / datapermission / desensitize）。
 * 通过 {@code framework.extras.enabled=false} 可全局禁用。
 *
 * <p>所有子能力的 Bean 注册均由 {@link ExtrasBootstrapConfiguration} 统一处理，
 * 各子能力相互独立（{@code @ConditionalOnClass}），缺省 optional 依赖时仅不注册对应 Bean。
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableExtras
 * public class MyApp { }
 * }</pre>
 *
 * @author Jowen
 * @date 2026-08-22
 * @see ExtrasBootstrapConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ExtrasBootstrapConfiguration.class)
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", matchIfMissing = true)
public @interface EnableExtras {
}
