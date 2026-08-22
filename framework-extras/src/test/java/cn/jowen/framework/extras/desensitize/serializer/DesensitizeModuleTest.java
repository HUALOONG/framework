/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.desensitize.serializer;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.extras.desensitize.DesensitizeSkipContextKey;
import cn.jowen.framework.extras.desensitize.annotation.CustomDesensitize;
import cn.jowen.framework.extras.desensitize.annotation.IdCardDesensitize;
import cn.jowen.framework.extras.desensitize.annotation.PhoneDesensitize;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DesensitizeModuleTest {

    /** 演示 VO：混合脱敏注解字段、普通字段、跳过字段、自定义字段。 */
    public static class DemoVO {
        @PhoneDesensitize
        public String phone = "13812345678";

        @IdCardDesensitize
        public String idCard = "11010519491231002X";

        public String plain = "secret-note";

        @PhoneDesensitize(skip = true)
        public String phoneSkip = "13900001111";

        @CustomDesensitize(startKeep = 2, endKeep = 2, replacement = "#")
        public String custom = "1234567890";
    }

    private static JsonMapper mapper() {
        return JsonMapper.builder().addModule(new DesensitizeModule()).build();
    }

    @Test
    void masksAnnotatedStringFields() throws Exception {
        String json = mapper().writeValueAsString(new DemoVO());

        assertThat(json).contains("\"phone\":\"138****5678\"");
        assertThat(json).contains("\"idCard\":\"110***********002X\"");
    }

    @Test
    void leavesNonAnnotatedStringFieldRaw() throws Exception {
        String json = mapper().writeValueAsString(new DemoVO());

        assertThat(json).contains("\"plain\":\"secret-note\"");
    }

    @Test
    void customStrategyHonorsKeepAndReplacement() throws Exception {
        String json = mapper().writeValueAsString(new DemoVO());

        assertThat(json).contains("\"custom\":\"12######90\"");
    }

    @Test
    void fieldLevelSkipKeepsOriginal() throws Exception {
        String json = mapper().writeValueAsString(new DemoVO());

        assertThat(json).contains("\"phoneSkip\":\"13900001111\"");
    }

    @Test
    void globalSkipContextOutputsPlainText() throws Exception {
        JsonMapper mapper = mapper();
        DemoVO vo = new DemoVO();

        ContextCarrier.runWith(DesensitizeSkipContextKey.DESENSITIZE_SKIP, true, () -> {
            try {
                String json = mapper.writeValueAsString(vo);
                assertThat(json).contains("\"phone\":\"13812345678\"");
                assertThat(json).contains("\"idCard\":\"11010519491231002X\"");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
