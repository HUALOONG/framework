package cn.jowen.framework.core.desensitize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * {@link Desensitizer}/{@link DesensitizeStrategies}/{@link DesensitizeContext} 测试。
 */
class DesensitizerTest {

    @Test
    void context_mask_keepsEdges() {
        DesensitizeContext ctx = DesensitizeContext.of(3, 4);
        assertThat(ctx.mask("13812345678")).isEqualTo("138****5678");
    }

    @Test
    void context_mask_blankReturnsRaw() {
        assertThat(DesensitizeContext.DEFAULT.mask("  ")).isEqualTo("  ");
        assertThat(DesensitizeContext.DEFAULT.mask(null)).isNull();
    }

    @Test
    void context_mask_skipReturnsRaw() {
        DesensitizeContext ctx = new DesensitizeContext(3, 4, "*", true);
        assertThat(ctx.mask("13812345678")).isEqualTo("13812345678");
    }

    @Test
    void strategy_phone_masksByDefault() {
        assertThat(DesensitizeStrategies.PHONE.mask("13812345678")).isEqualTo("138****5678");
    }

    @Test
    void strategy_invalidFormat_returnsRaw() {
        assertThat(DesensitizeStrategies.PHONE.mask("not-a-phone")).isEqualTo("not-a-phone");
    }

    @Test
    void strategy_idCard_masks() {
        assertThat(DesensitizeStrategies.ID_CARD.mask("11010519491231002X"))
                .isEqualTo("110***********002X");
    }

    @Test
    void strategy_email_keepsDomain() {
        assertThat(DesensitizeStrategies.EMAIL.mask("zhangsan@example.com"))
                .isEqualTo("z***@example.com");
    }

    @Test
    void desensitizer_maskByStrategyName() {
        Desensitizer desensitizer = Desensitizer.getInstance();
        assertThat(desensitizer.mask("13812345678", "phone")).isEqualTo("138****5678");
    }

    @Test
    void desensitizer_unknownStrategy_throws() {
        assertThatThrownBy(() -> Desensitizer.getInstance().mask("x", "NO_SUCH"))
                .isInstanceOf(DesensitizeException.class)
                .hasMessageContaining("未知脱敏策略");
    }

    @Test
    void desensitizer_maskObject_byAnnotation() {
        UserVO vo = new UserVO();
        vo.phone = "13812345678";
        vo.idCard = "11010519491231002X";
        vo.note = "keep-me";

        Desensitizer.getInstance().maskObject(vo);

        assertThat(vo.phone).isEqualTo("138****5678");
        assertThat(vo.idCard).isEqualTo("110***********002X");
        assertThat(vo.note).isEqualTo("keep-me");
    }

    @Test
    void desensitizer_maskObject_nullSafe() {
        assertThat(Desensitizer.getInstance().maskObject(null)).isNull();
    }

    /** 测试载体：带 {@link DesensitizeField} 注解的字段。 */
    static class UserVO {
        @DesensitizeField(strategy = "PHONE")
        String phone;

        @DesensitizeField(strategy = "ID_CARD", startKeep = 3, endKeep = 4)
        String idCard;

        String note;
    }
}
