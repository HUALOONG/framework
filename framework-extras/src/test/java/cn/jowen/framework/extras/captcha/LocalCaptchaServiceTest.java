/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha;

import cn.jowen.framework.extras.captcha.store.CaptchaStore;
import cn.jowen.framework.extras.captcha.store.LocalCaptchaStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalCaptchaServiceTest {

    /** 记录答案的存储间谍，便于测试校验正确码。 */
    static final class SpyCaptchaStore implements CaptchaStore {
        private final CaptchaStore delegate = new LocalCaptchaStore();
        private final Map<String, String> answers = new ConcurrentHashMap<>();

        @Override
        public void put(String id, String answer, long ttlMillis) {
            answers.put(id, answer);
            delegate.put(id, answer, ttlMillis);
        }

        @Override
        public @Nullable String get(String id) {
            return delegate.get(id);
        }

        @Override
        public void remove(String id) {
            delegate.remove(id);
        }

        @Override
        public void clearExpired() {
            delegate.clearExpired();
        }

        String answerOf(String id) {
            return answers.get(id);
        }
    }

    @Test
    void generateImageReturnsValidResult() {
        SpyCaptchaStore store = new SpyCaptchaStore();
        LocalCaptchaService service = new LocalCaptchaService(store);

        CaptchaResult result = service.generate(CaptchaType.IMAGE);

        assertThat(result.captchaId()).isNotBlank();
        assertThat(result.imageBase64())
                .startsWith("data:image/png;base64,")
                .hasSizeGreaterThan("data:image/png;base64,".length());
        assertThat(result.expiresIn()).isPositive();
        assertThat(store.answerOf(result.captchaId())).isNotNull();
    }

    @Test
    void verifyWithCorrectAndWrongCode() {
        SpyCaptchaStore store = new SpyCaptchaStore();
        LocalCaptchaService service = new LocalCaptchaService(store);

        CaptchaResult result = service.generate(CaptchaType.IMAGE);
        String code = store.answerOf(result.captchaId());

        assertThat(service.verify(result.captchaId(), code)).isTrue();
        assertThat(service.verify(result.captchaId(), "WRONG")).isFalse();
    }

    @Test
    void verifyDeleteAfterConsumesOnce() {
        SpyCaptchaStore store = new SpyCaptchaStore();
        LocalCaptchaService service = new LocalCaptchaService(store);

        CaptchaResult result = service.generate(CaptchaType.IMAGE);
        String code = store.answerOf(result.captchaId());

        assertThat(service.verify(result.captchaId(), code, true)).isTrue();
        // 删除后再次校验应失败
        assertThat(service.verify(result.captchaId(), code, true)).isFalse();
    }

    @Test
    void verifyKeepsAfterWhenDeleteAfterFalse() {
        SpyCaptchaStore store = new SpyCaptchaStore();
        LocalCaptchaService service = new LocalCaptchaService(store);

        CaptchaResult result = service.generate(CaptchaType.IMAGE);
        String code = store.answerOf(result.captchaId());

        assertThat(service.verify(result.captchaId(), code, false)).isTrue();
        assertThat(service.verify(result.captchaId(), code, false)).isTrue();
    }

    @Test
    void verifyExpiredEntryFails() throws InterruptedException {
        CaptchaProperties props = new CaptchaProperties();
        props.setTtlMillis(1);
        LocalCaptchaStore store = new LocalCaptchaStore();
        LocalCaptchaService service = new LocalCaptchaService(store, props);

        CaptchaResult result = service.generate(CaptchaType.IMAGE);
        Thread.sleep(5);
        store.clearExpired();

        assertThat(service.verify(result.captchaId(), "anything")).isFalse();
    }

    @Test
    void generateUnsupportedTypesThrow() {
        LocalCaptchaService service = new LocalCaptchaService(new LocalCaptchaStore());

        assertThatThrownBy(() -> service.generate(CaptchaType.SLIDER))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> service.generate(CaptchaType.SMS))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void captchaTypeHasFourValues() {
        assertThat(CaptchaType.values()).containsExactlyInAnyOrder(
                CaptchaType.IMAGE, CaptchaType.ARITHMETIC, CaptchaType.SLIDER, CaptchaType.SMS);
    }
}
