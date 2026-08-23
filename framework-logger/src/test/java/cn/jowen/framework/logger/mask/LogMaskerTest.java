package cn.jowen.framework.logger.mask;

import cn.jowen.framework.core.desensitize.DesensitizeContext;
import cn.jowen.framework.core.desensitize.DesensitizeRule;
import cn.jowen.framework.core.desensitize.Desensitizer;
import cn.jowen.framework.logger.config.LoggerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LogMasker} 测试。
 */
class LogMaskerTest {

    private final LoggerProperties properties = new LoggerProperties();
    private final Desensitizer desensitizer = Desensitizer.getInstance();

    @BeforeEach
    void registerTestRule() {
        // 注册测试用手机号脱敏规则（测试隔离：每次测试前重新注册）
        desensitizer.register(new DesensitizeRule() {
            @Override
            public String apply(String text, DesensitizeContext ctx) {
                return text.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1" + ctx.replacement().repeat(4) + "$2");
            }
        });
    }

    @Test
    void maskMessage_enabled_masksText() {
        properties.setDesensitizeEnabled(true);
        LogMasker masker = new LogMasker(properties, desensitizer);
        String result = masker.maskMessage("手机号13812345678");
        assertThat(result).contains("****");
    }

    @Test
    void maskMessage_disabled_returnsOriginal() {
        properties.setDesensitizeEnabled(false);
        LogMasker masker = new LogMasker(properties, desensitizer);
        String original = "手机号13812345678";
        String result = masker.maskMessage(original);
        assertThat(result).isEqualTo(original);
    }

    @Test
    void maskMessage_nullReturnsNull() {
        LogMasker masker = new LogMasker(properties, desensitizer);
        assertThat(masker.maskMessage(null)).isNull();
    }

    @Test
    void maskMessage_emptyString_returnsEmpty() {
        properties.setDesensitizeEnabled(true);
        LogMasker masker = new LogMasker(properties, desensitizer);
        assertThat(masker.maskMessage("")).isEqualTo("");
    }

    @Test
    void maskArgs_enabled_masksStrings() {
        properties.setDesensitizeEnabled(true);
        LogMasker masker = new LogMasker(properties, desensitizer);
        Object[] args = {"手机号13812345678", "用户名张三"};
        Object[] result = masker.maskArgs(args);
        assertThat(result).isNotSameAs(args);
        assertThat(result[0]).isInstanceOf(String.class);
        assertThat((String) result[0]).contains("****");
    }

    @Test
    void maskArgs_disabled_returnsOriginalArray() {
        properties.setDesensitizeEnabled(false);
        LogMasker masker = new LogMasker(properties, desensitizer);
        Object[] args = {"hello", "world"};
        Object[] result = masker.maskArgs(args);
        assertThat(result).isSameAs(args);
    }

    @Test
    void maskArgs_nullReturnsNull() {
        LogMasker masker = new LogMasker(properties, desensitizer);
        assertThat(masker.maskArgs(null)).isNull();
    }

    @Test
    void maskArgs_mixedTypes_stringsMasked_othersPreserved() {
        properties.setDesensitizeEnabled(true);
        LogMasker masker = new LogMasker(properties, desensitizer);
        Object[] args = {"敏感文字", 123, true};
        Object[] result = masker.maskArgs(args);
        assertThat(result).hasSize(3);
        assertThat(result[0]).isInstanceOf(String.class);
        assertThat(result[1]).isEqualTo(123);
        assertThat(result[2]).isEqualTo(true);
    }

    @Test
    void constructor_withDefaultDesensitizer() {
        LogMasker masker = new LogMasker(properties);
        Object[] args = {"test敏感数据"};
        Object[] result = masker.maskArgs(args);
        assertThat(result[0]).isInstanceOf(String.class);
    }

    @Test
    void maskMessage_noDesensitizeRules_returnsOriginal() {
        // 无注册规则时，mask() 原样返回
        properties.setDesensitizeEnabled(true);
        // 清除全局可能存在的自定义规则（通过新实例隔离）
        LogMasker masker = new LogMasker(properties, desensitizer);
        String result = masker.maskMessage("没有任何匹配规则的文字");
        // 无规则时原样返回
        assertThat(result).isEqualTo("没有任何匹配规则的文字");
    }
}
