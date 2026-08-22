/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableExtras} 注解基础测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class EnableExtrasTest {

    @Test
    void retentionAndTargets() {
        assertTrue(EnableExtras.class.isAnnotationPresent(Retention.class));
        assertTrue(EnableExtras.class.isAnnotationPresent(Target.class));
        assertTrue(EnableExtras.class.isAnnotationPresent(Documented.class));
        assertTrue(EnableExtras.class.isAnnotationPresent(Import.class));
        assertTrue(EnableExtras.class.isAnnotationPresent(ConditionalOnProperty.class));
    }

    @Test
    void importPointsToBootstrapConfiguration() {
        Import importAnn = EnableExtras.class.getAnnotation(Import.class);
        assertTrue(importAnn != null);
        boolean hasBootstrap = false;
        for (Class<?> c : importAnn.value()) {
            if (c == ExtrasBootstrapConfiguration.class) {
                hasBootstrap = true;
                break;
            }
        }
        assertTrue(hasBootstrap, "EnableExtras must import ExtrasBootstrapConfiguration");
    }

    @Test
    void conditionalOnPropertyExists() {
        ConditionalOnProperty cop = EnableExtras.class.getAnnotation(ConditionalOnProperty.class);
        assertTrue(cop != null);
        assertTrue(cop.matchIfMissing());
    }
}
