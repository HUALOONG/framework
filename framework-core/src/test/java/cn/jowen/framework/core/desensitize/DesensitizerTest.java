package cn.jowen.framework.core.desensitize;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Desensitizer} 测试。
 */
class DesensitizerTest {

    Desensitizer desensitizer = Desensitizer.getInstance();

    @Test
    void getInstance_returnsSame() {
        assertThat(Desensitizer.getInstance()).isSameAs(Desensitizer.getInstance());
    }

    @Test
    void mask_autoDetect_phone() {
        // mask(String) 只跑 SPI 自定义规则，无注册规则时原样返回
        assertThat(desensitizer.mask("13812345678")).isEqualTo("13812345678");
    }

    @Test
    void mask_autoDetect_email() {
        assertThat(desensitizer.mask("user@test.com")).isEqualTo("user@test.com");
    }

    @Test
    void mask_nullReturnsNull() {
        assertThat(desensitizer.mask(null)).isNull();
    }

    @Test
    void mask_byStrategy() {
        assertThat(desensitizer.mask("13812345678", "phone")).isEqualTo("138****5678");
    }

    @Test
    void mask_byStrategy_caseInsensitive() {
        assertThat(desensitizer.mask("13812345678", "PHONE")).isEqualTo("138****5678");
    }

    @Test
    void mask_byStrategy_nullOrBlankReturnsRaw() {
        assertThat(desensitizer.mask(null, "PHONE")).isNull();
        assertThat(desensitizer.mask("", "PHONE")).isEqualTo("");
        assertThat(desensitizer.mask("  ", "PHONE")).isEqualTo("  ");
    }

    @Test
    void mask_unknownStrategy_throws() {
        assertThatThrownBy(() -> desensitizer.mask("x", "UNKNOWN"))
                .isInstanceOf(DesensitizeException.class)
                .hasMessageContaining("未知脱敏策略");
    }

    @Test
    void mask_byStrategyWithContext() {
        DesensitizeContext ctx = DesensitizeContext.of(2, 2);
        assertThat(desensitizer.mask("12345678", "CUSTOM", ctx)).isEqualTo("12****78");
    }

    @Test
    void register_customRule_applied() {
        Desensitizer testInstance = Desensitizer.getInstance();
        testInstance.register((text, ctx) -> text.replace("敏感", "****"));
        assertThat(testInstance.mask("包含敏感词")).contains("****");
    }

    @Test
    void maskObject_nullReturnsNull() {
        assertThat(desensitizer.maskObject(null)).isNull();
    }

    @Test
    void maskObject_desensitizesAnnotatedField() {
        UserVO user = new UserVO();
        user.phone = "13812345678";
        user.idCard = "11010519491231002X";

        Object result = desensitizer.maskObject(user);

        assertThat(result).isSameAs(user);
        assertThat(user.phone).isEqualTo("138****5678");
        assertThat(user.idCard).isEqualTo("110***********002X");
    }

    @Test
    void maskObject_skipsStaticAndFinal() {
        UserVOWithStatic vo = new UserVOWithStatic();
        assertThat(desensitizer.maskObject(vo)).isSameAs(vo);
    }

    @Test
    void maskObject_unknownStrategyInAnnotation_throws() {
        UserVOInvalid user = new UserVOInvalid();
        user.phone = "13812345678";

        assertThatThrownBy(() -> desensitizer.maskObject(user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maskObject_skipsUnannotatedNullAndBlankFields() {
        // 无注解字段、null 字段、空白字段均须跳过；keep 字段作为对照证明跳过判定精确
        UserVOWithSkippedFields vo = new UserVOWithSkippedFields();

        Object result = desensitizer.maskObject(vo);

        assertThat(result).isSameAs(vo);
        assertThat(vo.nickname).isEqualTo("张三");
        assertThat(vo.phone).isNull();
        assertThat(vo.empty).isEqualTo("   ");
        assertThat(vo.keep).isEqualTo("138****5678");
    }

    @Test
    void maskMap_nullReturnsNull() {
        assertThat(desensitizer.maskMap(null, "PHONE")).isNull();
    }

    @Test
    void maskMap_emptyMapReturnsEmpty() {
        assertThat(desensitizer.maskMap(Collections.emptyMap(), "PHONE")).isEmpty();
    }

    @Test
    void maskMap_masksStringValues() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("phone", "13812345678");
        source.put("email", "user@test.com");
        source.put("name", "张三");

        // maskMap 按 CUSTOM 策略逐一脱敏（默认 0/0 全量脱敏）
        Map<String, String> result = desensitizer.maskMap(source, "CUSTOM");

        assertThat(result.get("phone")).isEqualTo("***********");  // 11 位全量
        assertThat(result.get("email")).isEqualTo("*************"); // 13 位全量
        assertThat(result.get("name")).isEqualTo("**");             // 2 位全量
        assertThat(result).hasSize(3);
    }

    static class UserVO {
        @DesensitizeField(strategy = "PHONE")
        String phone;

        @DesensitizeField(strategy = "ID_CARD")
        String idCard;
    }

    static class UserVOWithStatic {
        @DesensitizeField(strategy = "PHONE")
        static String staticField = "13812345678";

        @DesensitizeField(strategy = "PHONE")
        final String finalField = "13900000000";
    }

    static class UserVOInvalid {
        @DesensitizeField(strategy = "NO_SUCH_STRATEGY")
        String phone;
    }

    static class UserVOWithSkippedFields {
        String nickname = "张三"; // 无 @DesensitizeField：注解为空跳过

        @DesensitizeField(strategy = "PHONE")
        String phone; // null：原值跳过

        @DesensitizeField(strategy = "PHONE")
        String empty = "   "; // 空白：原值跳过

        @DesensitizeField(strategy = "PHONE")
        String keep = "13812345678"; // 对照字段：应正常脱敏
    }
}
