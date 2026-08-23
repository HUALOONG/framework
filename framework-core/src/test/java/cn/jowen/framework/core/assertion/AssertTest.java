package cn.jowen.framework.core.assertion;

import cn.jowen.framework.core.exception.BusinessException;
import cn.jowen.framework.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssertTest {

    enum TestCode implements ErrorCode {
        CODE("1001", "测试错误");

        TestCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        private final String code;
        private final String message;

        @Override
        public String code() {
            return code;
        }

        @Override
        public String message() {
            return message;
        }
    }

    @Test
    void notNull_successWhenNotNull() {
        Assert.notNull("hello", null, null);
    }

    @Test
    void notNull_throwsWhenNull() {
        assertThatThrownBy(() -> Assert.notNull(null, null, "null error"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("null error");
    }

    @Test
    void notNull_defaultMessage() {
        assertThatThrownBy(() -> Assert.notNull(null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("对象不能为 null");
    }

    @Test
    void notNull_withErrorCode() {
        assertThatThrownBy(() -> Assert.notNull(null, TestCode.CODE, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("测试错误");
    }

    @Test
    void notEmpty_string_successWhenNotEmpty() {
        Assert.notEmpty("hello", null, null);
    }

    @Test
    void notEmpty_string_throwsWhenNull() {
        assertThatThrownBy(() -> Assert.notEmpty((String) null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("字符串不能为空");
    }

    @Test
    void notEmpty_string_throwsWhenBlank() {
        assertThatThrownBy(() -> Assert.notEmpty("   ", null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void notEmpty_collection_successWhenNotEmpty() {
        Assert.notEmpty(List.of("a", "b"), null, null);
    }

    @Test
    void notEmpty_collection_throwsWhenNull() {
        assertThatThrownBy(() -> Assert.notEmpty((List<?>) null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("集合不能为空");
    }

    @Test
    void notEmpty_collection_throwsWhenEmpty() {
        assertThatThrownBy(() -> Assert.notEmpty(List.of(), null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void notEmpty_map_successWhenNotEmpty() {
        Assert.notEmpty(Map.of("k", "v"), null, null);
    }

    @Test
    void notEmpty_map_throwsWhenNull() {
        assertThatThrownBy(() -> Assert.notEmpty((Map<?, ?>) null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Map 不能为空");
    }

    @Test
    void notEmpty_map_throwsWhenEmpty() {
        assertThatThrownBy(() -> Assert.notEmpty(Map.of(), null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void isTrue_successWhenTrue() {
        Assert.isTrue(true, null, null);
    }

    @Test
    void isTrue_throwsWhenFalse() {
        assertThatThrownBy(() -> Assert.isTrue(false, null, "条件不满足"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("条件不满足");
    }

    @Test
    void isTrue_defaultMessage() {
        assertThatThrownBy(() -> Assert.isTrue(false, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("条件校验失败");
    }
}
