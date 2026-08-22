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
import cn.jowen.framework.extras.notification.config.SmsProperties;
import org.junit.jupiter.api.Test;

/**
 * {@link SmsNotificationHandler} 基础行为测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class SmsNotificationHandlerTest {

    @Test
    void supportsSmsChannel() {
        SmsProperties props = new SmsProperties();
        SmsNotificationHandler handler = new SmsNotificationHandler(props);
        assertTrue(handler.supports(NotificationChannel.SMS));
    }

    @Test
    void doesNotSupportOtherChannels() {
        SmsProperties props = new SmsProperties();
        SmsNotificationHandler handler = new SmsNotificationHandler(props);
        assertFalse(handler.supports(NotificationChannel.EMAIL));
        assertFalse(handler.supports(NotificationChannel.WEBHOOK));
    }

    @Test
    void sendFailsWhenAccessKeyIdMissing() {
        SmsProperties props = new SmsProperties();
        // accessKeyId is null by default
        SmsNotificationHandler handler = new SmsNotificationHandler(props);
        NotificationRequest req = NotificationRequest.builder()
                .channel(NotificationChannel.SMS)
                .to("13800138000")
                .content("test message")
                .build();
        var result = handler.send(req);
        assertNotNull(result);
        assertTrue(!result.success());
    }
}
