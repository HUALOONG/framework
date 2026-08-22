/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

/**
 * Excel 忽略字段注解，标记在不需要导出/导入的字段上。
 *
 * <p>与 EasyExcel 的 {@code @ExcelIgnore} 功能相似，但作为框架自定义注解，
 * 不依赖 EasyExcel 类路径，可在无 Excel 依赖时使用。
 *
 * @author Jowen
 * @date 2026-08-22
 * @see com.alibaba.excel.annotation.ExcelIgnore
 */
@NullMarked
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelIgnore {
}
