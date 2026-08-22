/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification.channel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.config.EmailProperties;
import org.junit.jupiter.api.Test;

/**
 * {@link EmailNotificationHandler} 基础行为测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class EmailNotificationHandlerTest {

    @Test
    void supportsEmailChannel() {
        EmailProperties props = new EmailProperties();
        EmailNotificationHandler handler = new EmailNotificationHandler(props);
        assertTrue(handler.supports(NotificationChannel.EMAIL));
    }

    @Test
    void doesNotSupportOtherChannels() {
        EmailProperties props = new EmailProperties();
        EmailNotificationHandler handler = new EmailNotificationHandler(props);
        assertFalse(handler.supports(NotificationChannel.WEBHOOK));
        assertFalse(handler.supports(NotificationChannel.SMS));
        assertFalse(handler.supports(NotificationChannel.DINGTALK));
        assertFalse(handler.supports(NotificationChannel.WECHAT_WORK));
    }

    @Test
    void sendFailsWhenHostMissing() {
        EmailProperties props = new EmailProperties();
        // host is null by default
        EmailNotificationHandler handler = new EmailNotificationHandler(props);
        NotificationRequest req = NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .to("test@example.com")
                .subject("test")
                .content("body")
                .build();
        // Should not throw; returns failure result
        var result = handler.send(req);
        assertNotNull(result);
        assertTrue(!result.success());
    }

    @Test
    void sendFailsWhenToMissing() {
        EmailProperties props = new EmailProperties();
        props.setHost("smtp.example.com");
        EmailNotificationHandler handler = new EmailNotificationHandler(props);
        NotificationRequest req = NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .subject("test")
                .content("body")
                .build();
        var result = handler.send(req);
        assertNotNull(result);
        assertTrue(!result.success());
    }
}
