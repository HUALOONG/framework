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
 * 数据权限范围枚举。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public enum DataScope {
    /** 全部数据。 */
    ALL,
    /** 本部门及子部门。 */
    DEPT_AND_CHILD,
    /** 仅本部门。 */
    DEPT,
    /** 仅本人。 */
    SELF,
    /** 自定义。 */
    CUSTOM
}
