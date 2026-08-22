/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification;

import cn.jowen.framework.extras.notification.channel.ConsoleNotificationHandler;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalNotificationServiceTest {

    private static NotificationRequest sample(NotificationChannel channel) {
        return NotificationRequest.builder()
                .channel(channel)
                .to("user@example.com")
                .subject("Hi")
                .content("Hello")
                .build();
    }

    @Test
    void sendDelegatesToRegisteredHandler() {
        LocalNotificationService service =
                new LocalNotificationService(List.of(new ConsoleNotificationHandler()));

        NotificationResult result = service.send(sample(NotificationChannel.EMAIL));

        assertThat(result.success()).isTrue();
        assertThat(result.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.messageId()).isNotNull();
    }

    @Test
    void sendUnregisteredChannelReturnsFailure() {
        LocalNotificationService service = new LocalNotificationService();

        NotificationResult result = service.send(sample(NotificationChannel.EMAIL));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("未注册");
    }

    @Test
    void sendAsyncCompletesSuccessfully() throws Exception {
        LocalNotificationService service =
                new LocalNotificationService(List.of(new ConsoleNotificationHandler()));
        try {
            NotificationResult result = service.sendAsync(sample(NotificationChannel.SMS))
                    .get(5, TimeUnit.SECONDS);
            assertThat(result.success()).isTrue();
        } finally {
            service.shutdown();
        }
    }

    @Test
    void sendBatchHandlesPartialFailureWithoutCrash() {
        // 仅支持 EMAIL；SMS 处理器抛异常，验证不影响其他结果
        NotificationChannelHandler emailOnly = new NotificationChannelHandler() {
            @Override
            public NotificationResult send(NotificationRequest req) {
                if (req.channel() == NotificationChannel.EMAIL) {
                    return NotificationResult.success(req.channel(), "mid");
                }
                throw new IllegalStateException("boom");
            }

            @Override
            public boolean supports(NotificationChannel channel) {
                return channel == NotificationChannel.EMAIL;
            }
        };
        LocalNotificationService service = new LocalNotificationService(List.of(emailOnly));

        List<NotificationResult> results = service.sendBatch(List.of(
                sample(NotificationChannel.EMAIL),
                sample(NotificationChannel.SMS)));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).success()).isTrue();
        assertThat(results.get(1).success()).isFalse();
    }
}
