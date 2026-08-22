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
 * 通知渠道处理器（SPI）：处理特定渠道的发送逻辑。
 *
 * <p>实现应返回 {@link NotificationResult} 而非抛出异常；未知/失败场景返回 {@code success=false}。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public interface NotificationChannelHandler {

    /**
     * 发送通知。
     *
     * @param req 请求
     * @return 结果（禁止抛异常）
     */
    NotificationResult send(NotificationRequest req);

    /**
     * 是否支持该渠道。
     *
     * @param channel 渠道
     * @return 支持返回 {@code true}
     */
    boolean supports(NotificationChannel channel);
}
