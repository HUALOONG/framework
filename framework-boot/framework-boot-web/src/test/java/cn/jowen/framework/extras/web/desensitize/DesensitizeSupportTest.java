package cn.jowen.framework.extras.web.desensitize;

import cn.jowen.framework.core.desensitize.DesensitizeField;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DesensitizeSupport} 返回值形态适配测试：
 * 覆盖单对象 / 不可变 List / 数组 / Map / null 与 skip 字段。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class DesensitizeSupportTest {

    /** 测试 VO。 */
    public static class UserVO {
        /** 姓名（不脱敏）。 */
        public String name;

        /** 手机号。 */
        @DesensitizeField(strategy = "PHONE")
        public String phone;

        /** 身份证号。 */
        @DesensitizeField(strategy = "ID_CARD")
        public String idCard;

        /** 邮箱（跳过脱敏）。 */
        @DesensitizeField(strategy = "EMAIL", skip = true)
        public String email;

        UserVO(String name, String phone, String idCard, String email) {
            this.name = name;
            this.phone = phone;
            this.idCard = idCard;
            this.email = email;
        }
    }

    private static UserVO sample() {
        return new UserVO("张三", "13812345678", "340101199001011234", "zhangsan@example.com");
    }

    @Test
    void mask_pojo_masksAnnotatedFieldsOnly() {
        UserVO vo = sample();
        Object masked = DesensitizeSupport.mask(vo);

        assertThat(masked).isSameAs(vo);
        assertThat(vo.name).isEqualTo("张三");
        assertThat(vo.phone).isEqualTo("138****5678");
        assertThat(vo.idCard).isEqualTo("340***********1234");
    }

    @Test
    void mask_skipAnnotation_leftUntouched() {
        UserVO vo = sample();
        DesensitizeSupport.mask(vo);
        // skip=true：邮箱明文保留
        assertThat(vo.email).isEqualTo("zhangsan@example.com");
    }

    @Test
    void mask_null_returnsNull() {
        assertThat(DesensitizeSupport.mask(null)).isNull();
    }

    @Test
    void mask_immutableList_copiedAndElementsMasked() {
        List<UserVO> list = List.of(sample(), sample());
        List<?> masked = (List<?>) DesensitizeSupport.mask(list);

        assertThat(masked).hasSize(2);
        assertThat(masked).isNotSameAs(list);
        // 原列表持有同一批元素（反射原地脱敏），副本列表本身不影响原集合语义
        assertThat(masked.get(0)).isInstanceOf(UserVO.class);
        assertThat(((UserVO) masked.get(0)).phone).isEqualTo("138****5678");
    }

    @Test
    void mask_array_returnsNewMaskedArray() {
        UserVO[] array = {sample()};
        Object masked = DesensitizeSupport.mask(array);

        assertThat(masked).isInstanceOf(UserVO[].class);
        assertThat(((UserVO[]) masked)).hasSize(1);
        assertThat(((UserVO[]) masked)[0].phone).isEqualTo("138****5678");
    }

    @Test
    void mask_map_stringValuesMasked_othersKept() {
        Map<String, Object> map = new HashMap<>();
        map.put("phone", "13812345678");
        map.put("count", 42);
        map.put("remark", "普通文本不脱敏");

        Map<?, ?> masked = (Map<?, ?>) DesensitizeSupport.mask(map);

        assertThat(masked.get("phone")).isEqualTo("138****5678");
        assertThat(masked.get("count")).isEqualTo(42);
        // 格式不符的普通文本原样保留，不误伤
        assertThat(masked.get("remark")).isEqualTo("普通文本不脱敏");
    }
}
