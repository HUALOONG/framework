/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha.generator;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArithmeticCaptchaGeneratorTest {

    @RepeatedTest(20)
    void problemAnswerIsSelfConsistent() {
        ArithmeticCaptchaGenerator generator = new ArithmeticCaptchaGenerator();
        ArithmeticCaptchaGenerator.ArithmeticProblem problem = generator.buildProblem();

        int expected = problem.add() ? (problem.a() + problem.b()) : Math.abs(problem.a() - problem.b());
        assertThat(problem.answer()).isEqualTo(expected);
        assertThat(problem.expression()).contains("=");
    }

    @Test
    void generateProducesNonNullImageAndNumericAnswer() {
        ArithmeticCaptchaGenerator generator = new ArithmeticCaptchaGenerator();
        CaptchaImage image = generator.generate();

        assertThat(image.image()).isNotNull();
        assertThat(image.answer()).containsOnlyDigits();
    }
}
