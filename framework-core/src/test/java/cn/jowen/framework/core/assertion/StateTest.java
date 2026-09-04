package cn.jowen.framework.core.assertion;

import cn.jowen.framework.core.exception.ErrorCode;
import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link State} 测试。
 */
class StateTest {

    enum TestCode implements ErrorCode {
        STATE_ERR("2001", "状态错误");

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
    void checkState_successWhenTrue() {
        State.checkState(true);
    }

    @Test
    void checkState_throwsWhenFalse() {
        catchThrowableOfType(() -> State.checkState(false), SystemException.class);
    }

    @Test
    void checkState_throwsWhenFalse_withCode() {
        catchThrowableOfType(() -> State.checkState(false, TestCode.STATE_ERR, null), SystemException.class);
    }

    @Test
    void checkNotNull_successWhenNotNull() {
        String result = State.checkNotNull("hello", null, null);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void checkNotNull_throwsWhenNull() {
        catchThrowableOfType(() -> State.checkNotNull(null, null, "null check"), SystemException.class);
    }

    @Test
    void checkNotEmpty_string_success() {
        String result = State.checkNotEmpty("hello", null, null);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void checkNotEmpty_string_throwsWhenNull() {
        catchThrowableOfType(() -> State.checkNotEmpty((String) null, null, null), SystemException.class);
    }

    @Test
    void checkNotEmpty_string_throwsWhenBlank() {
        catchThrowableOfType(() -> State.checkNotEmpty("  ", null, null), SystemException.class);
    }

    @Test
    void checkNotEmpty_collection_success() {
        List<String> list = List.of("a", "b");
        assertThat(State.checkNotEmpty(list, null, null)).isSameAs(list);
    }

    @Test
    void checkNotEmpty_collection_throwsWhenEmpty() {
        catchThrowableOfType(() -> State.checkNotEmpty(List.of(), null, null), SystemException.class);
    }

    @Test
    void checkNotEmpty_map_success() {
        Map<String, String> map = Map.of("k", "v");
        assertThat(State.checkNotEmpty(map, null, null)).isSameAs(map);
    }

    @Test
    void checkNotEmpty_map_throwsWhenEmpty() {
        catchThrowableOfType(() -> State.checkNotEmpty(Map.of(), null, null), SystemException.class);
    }

    @Test
    void checkState_throwsWhenFalse_withMessageOnly() {
        // 覆盖 fail() 中 code 为 null 但 message 非空的兜底分支
        catchThrowableOfType(() -> State.checkState(false, null, "状态异常"), SystemException.class);
    }

    @Test
    void checkState_throwsWhenFalse_withCodeAndMessage() {
        catchThrowableOfType(() -> State.checkState(false, TestCode.STATE_ERR, "状态异常"), SystemException.class);
    }

    @Test
    void checkNotNull_throwsWithCodeAndMessage() {
        catchThrowableOfType(() -> State.checkNotNull(null, TestCode.STATE_ERR, "非空约束"), SystemException.class);
    }

    @Test
    void checkNotEmpty_string_throwsWithCodeAndMessage() {
        catchThrowableOfType(() -> State.checkNotEmpty((String) null, TestCode.STATE_ERR, "非空字符串"), SystemException.class);
    }

    @Test
    void checkNotEmpty_collection_throwsWithCodeAndMessage() {
        catchThrowableOfType(() -> State.checkNotEmpty(List.of(), TestCode.STATE_ERR, "非空集合"), SystemException.class);
    }

    @Test
    void checkNotEmpty_map_throwsWithCodeAndMessage() {
        catchThrowableOfType(() -> State.checkNotEmpty(Map.of(), TestCode.STATE_ERR, "非空映射"), SystemException.class);
    }

    @Test
    void checkState_false_throwsWithDefaultMessage() {
        // 显式断言异常类型与兜底消息，覆盖 fail() 中 code 为 null 且 message 为 null 的分支
        assertThatThrownBy(() -> State.checkState(false))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("系统状态校验失败");
    }

    @Test
    void checkNotNull_null_throwsWithDefaultMessage() {
        assertThatThrownBy(() -> State.checkNotNull(null, null, null))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("系统状态校验失败");
    }

    @Test
    void checkNotEmpty_stringBlank_throwsWithCode() {
        SystemException ex = assertThrows(SystemException.class,
                () -> State.checkNotEmpty("  ", TestCode.STATE_ERR, null));
        assertThat(ex.getErrorCode().code()).isEqualTo("2001");
    }

    @Test
    void checkNotEmpty_collectionEmpty_throwsWithCode() {
        SystemException ex = assertThrows(SystemException.class,
                () -> State.checkNotEmpty(List.of(), TestCode.STATE_ERR, null));
        assertThat(ex.getErrorCode().code()).isEqualTo("2001");
    }

    @Test
    void checkNotEmpty_mapEmpty_throwsWithCode() {
        SystemException ex = assertThrows(SystemException.class,
                () -> State.checkNotEmpty(Map.of(), TestCode.STATE_ERR, null));
        assertThat(ex.getErrorCode().code()).isEqualTo("2001");
    }
}