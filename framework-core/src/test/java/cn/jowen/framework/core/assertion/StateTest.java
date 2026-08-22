package cn.jowen.framework.core.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.jowen.framework.core.exception.SystemException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link State} 状态断言测试。
 */
class StateTest {

    @Test
    void checkState_passes() {
        assertThatCode(() -> State.checkState(true)).doesNotThrowAnyException();
    }

    @Test
    void checkState_fails() {
        assertThatThrownBy(() -> State.checkState(false))
                .isInstanceOf(SystemException.class);
    }

    @Test
    void checkState_failsWithMessage() {
        assertThatThrownBy(() -> State.checkState(false, null, "组件未就绪"))
                .isInstanceOf(SystemException.class)
                .hasMessage("组件未就绪");
    }

    @Test
    void checkNotNull_returnsObject() {
        String value = State.checkNotNull("ok", null, null);
        assertThat(value).isEqualTo("ok");
    }

    @Test
    void checkNotNull_fails() {
        assertThatThrownBy(() -> State.checkNotNull(null, null, "值不能为空"))
                .isInstanceOf(SystemException.class);
    }

    @Test
    void checkNotEmpty_string() {
        assertThatThrownBy(() -> State.checkNotEmpty("  ", null, "blank"))
                .isInstanceOf(SystemException.class);
        assertThat(State.checkNotEmpty("x", null, null)).isEqualTo("x");
    }

    @Test
    void checkNotEmpty_collectionAndMap() {
        assertThatThrownBy(() -> State.checkNotEmpty(List.of(), null, "empty list"))
                .isInstanceOf(SystemException.class);
        assertThatThrownBy(() -> State.checkNotEmpty(Map.of(), null, "empty map"))
                .isInstanceOf(SystemException.class);
        assertThat(State.checkNotEmpty(List.of(1), null, null)).containsExactly(1);
    }
}
