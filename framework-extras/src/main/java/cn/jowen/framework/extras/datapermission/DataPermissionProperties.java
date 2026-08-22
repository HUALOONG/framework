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
 * 数据权限配置载体（轻量 POJO，非 Spring {@code @ConfigurationProperties}）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class DataPermissionProperties {

    /** 是否启用数据权限。 */
    private boolean enabled = true;

    /** 默认数据范围（未配置用户时回退）。 */
    private DataScope defaultScope = DataScope.ALL;

    public DataPermissionProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataScope getDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(DataScope defaultScope) {
        this.defaultScope = defaultScope;
    }
}
