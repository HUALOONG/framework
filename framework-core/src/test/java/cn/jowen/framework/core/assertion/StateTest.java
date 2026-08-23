package cn.jowen.framework.core.assertion;

import cn.jowen.framework.core.exception.ErrorCode;
import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

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
}