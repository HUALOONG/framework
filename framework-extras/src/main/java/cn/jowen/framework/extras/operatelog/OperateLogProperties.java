/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.operatelog;

import org.jspecify.annotations.NullMarked;

/**
 * 操作日志配置载体（轻量 POJO，非 Spring {@code @ConfigurationProperties}）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class OperateLogProperties {

    /** 是否启用操作日志。 */
    private boolean enabled = true;

    /** 默认是否异步分发。 */
    private boolean async = true;

    public OperateLogProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
