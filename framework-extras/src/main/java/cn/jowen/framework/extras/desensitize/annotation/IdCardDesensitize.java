/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.desensitize.annotation;

import cn.jowen.framework.extras.desensitize.serializer.DesensitizeJsonSerializer;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * 身份证号脱敏注解（便捷封装），等价于元标注 {@link DesensitizeMeta#strategy() = "ID_CARD"}，
 * 输出如 {@code 110***********002X}。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@DesensitizeMeta(strategy = "ID_CARD")
@JsonSerialize(using = DesensitizeJsonSerializer.class)
public @interface IdCardDesensitize {

    /** 是否跳过脱敏。 */
    boolean skip() default false;
}
