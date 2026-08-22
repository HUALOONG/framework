/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;

/**
 * 日志通知处理器：将通知内容写入 JDK 自带日志（{@link Logger}），用于本地调试。
 *
 * <p>零外部依赖（使用 {@code java.util.logging}，无需引入第三方日志门面），
 * 不真实发送。支持全部渠道（调试用途）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class LogNotificationHandler implements NotificationChannelHandler {

    private final Logger logger = Logger.getLogger(LogNotificationHandler.class.getName());

    @Override
    public NotificationResult send(NotificationRequest req) {
        String message = "channel=" + req.channel()
                + (req.to() != null ? ", to=" + req.to() : "")
                + (req.subject() != null ? ", subject=" + req.subject() : "")
                + ", content=" + req.content();
        logger.log(Level.INFO, "[NOTIFY] " + message);
        return NotificationResult.success(req.channel(), UUID.randomUUID().toString());
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return true;
    }
}
