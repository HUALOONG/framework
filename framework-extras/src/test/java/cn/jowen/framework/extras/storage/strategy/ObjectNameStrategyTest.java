/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.strategy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectNameStrategyTest {

    @Test
    void datePathStrategyProducesDateDirectory() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate fixed = LocalDate.of(2026, 8, 22);
        DatePathStrategy strategy = new DatePathStrategy(fmt, () -> fixed);

        String name = strategy.generate("avatar.png");

        assertThat(name).startsWith("2026/08/22/");
        assertThat(name).endsWith("_avatar.png");
        assertThat(name).doesNotContain("..");
    }

    @Test
    void fourStrategiesProduceDifferentNames() {
        String file = "report.pdf";
        String date = new DatePathStrategy().generate(file);
        String hash = new HashStrategy().generate(file);
        String uuid = new UuidStrategy().generate(file);
        String original = new OriginalNameStrategy().generate(file);

        assertThat(date).isNotEqualTo(hash).isNotEqualTo(uuid).isNotEqualTo(original);
        assertThat(hash).isNotEqualTo(uuid).isNotEqualTo(original);
        assertThat(uuid).isNotEqualTo(original);
    }

    @Test
    void originalNameStrategyKeepsName() {
        assertThat(new OriginalNameStrategy().generate("a/b/c/file.csv")).isEqualTo("file.csv");
    }

    @Test
    void uuidStrategyKeepsExtension() {
        assertThat(new UuidStrategy().generate("photo.jpg")).endsWith(".jpg");
    }
}
