/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification;

import org.jspecify.annotations.NullMarked;

/**
 * 通知配置载体（轻量 POJO，非 Spring {@code @ConfigurationProperties}）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class NotificationProperties {

    /** 默认发送方标识。 */
    private String defaultFrom = "noreply@jowen.cn";

    /** 是否启用异步发送。 */
    private boolean asyncEnabled = true;

    public NotificationProperties() {
    }

    public String getDefaultFrom() {
        return defaultFrom;
    }

    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    public void setAsyncEnabled(boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }
}
