/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 通知结果。
 *
 * @param success       是否成功
 * @param messageId     消息标识（可为 null）
 * @param errorMessage  错误信息（失败时为非 null）
 * @param channel       渠道
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public record NotificationResult(boolean success, @Nullable String messageId,
        @Nullable String errorMessage, @Nullable NotificationChannel channel) {

    /** 构建成功结果。 */
    public static NotificationResult success(NotificationChannel channel, @Nullable String messageId) {
        return new NotificationResult(true, messageId, null, channel);
    }

    /** 构建失败结果。 */
    public static NotificationResult failure(NotificationChannel channel, String errorMessage) {
        return new NotificationResult(false, null, errorMessage, channel);
    }
}
