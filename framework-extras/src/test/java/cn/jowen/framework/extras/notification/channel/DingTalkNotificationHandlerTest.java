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
import cn.jowen.framework.extras.notification.config.DingTalkProperties;
import org.junit.jupiter.api.Test;

/**
 * {@link DingTalkNotificationHandler} 基础行为测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class DingTalkNotificationHandlerTest {

    @Test
    void supportsDingTalkChannel() {
        DingTalkProperties props = new DingTalkProperties();
        DingTalkNotificationHandler handler = new DingTalkNotificationHandler(props);
        assertTrue(handler.supports(NotificationChannel.DINGTALK));
    }

    @Test
    void doesNotSupportOtherChannels() {
        DingTalkProperties props = new DingTalkProperties();
        DingTalkNotificationHandler handler = new DingTalkNotificationHandler(props);
        assertFalse(handler.supports(NotificationChannel.EMAIL));
        assertFalse(handler.supports(NotificationChannel.WEBHOOK));
    }

    @Test
    void sendFailsWhenWebhookUrlMissing() {
        DingTalkProperties props = new DingTalkProperties();
        // webhookUrl is null by default
        DingTalkNotificationHandler handler = new DingTalkNotificationHandler(props);
        NotificationRequest req = NotificationRequest.builder()
                .channel(NotificationChannel.DINGTALK)
                .content("test message")
                .build();
        var result = handler.send(req);
        assertNotNull(result);
        assertTrue(!result.success());
    }
}
