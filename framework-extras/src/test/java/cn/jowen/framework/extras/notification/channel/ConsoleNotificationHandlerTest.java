/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleNotificationHandlerTest {

    @Test
    void sendWritesToSystemOutAndReturnsSuccess() {
        ConsoleNotificationHandler handler = new ConsoleNotificationHandler();
        NotificationRequest req = NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .to("user@example.com")
                .subject("Title")
                .content("Hello Console")
                .build();

        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            NotificationResult result = handler.send(req);
            assertThat(result.success()).isTrue();
        } finally {
            System.setOut(original);
        }

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("Hello Console").contains("EMAIL");
    }

    @Test
    void supportsAllChannels() {
        ConsoleNotificationHandler handler = new ConsoleNotificationHandler();
        for (NotificationChannel channel : NotificationChannel.values()) {
            assertThat(handler.supports(channel)).isTrue();
        }
    }
}
