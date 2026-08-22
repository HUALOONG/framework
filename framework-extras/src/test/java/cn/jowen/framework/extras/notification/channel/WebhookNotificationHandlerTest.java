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
import cn.jowen.framework.extras.notification.config.WebhookProperties;
import org.junit.jupiter.api.Test;

/**
 * {@link WebhookNotificationHandler} 基础行为测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class WebhookNotificationHandlerTest {

    @Test
    void supportsWebhookChannel() {
        WebhookProperties props = new WebhookProperties();
        WebhookNotificationHandler handler = new WebhookNotificationHandler(props);
        assertTrue(handler.supports(NotificationChannel.WEBHOOK));
    }

    @Test
    void doesNotSupportOtherChannels() {
        WebhookProperties props = new WebhookProperties();
        WebhookNotificationHandler handler = new WebhookNotificationHandler(props);
        assertFalse(handler.supports(NotificationChannel.EMAIL));
        assertFalse(handler.supports(NotificationChannel.SMS));
    }

    @Test
    void sendFailsWhenBaseUrlAndToMissing() {
        WebhookProperties props = new WebhookProperties();
        // baseUrl is null, no to in request
        WebhookNotificationHandler handler = new WebhookNotificationHandler(props);
        NotificationRequest req = NotificationRequest.builder()
                .channel(NotificationChannel.WEBHOOK)
                .subject("test")
                .content("body")
                .build();
        var result = handler.send(req);
        assertNotNull(result);
        assertTrue(!result.success());
    }
}
