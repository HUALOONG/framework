package cn.jowen.framework.data.mybatis.adapter;

import cn.jowen.framework.data.core.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlexExceptionTranslatorTest {

    @Test
    void getInstance_returnsSingleton() {
        assertThat(FlexExceptionTranslator.getInstance())
                .isSameAs(FlexExceptionTranslator.getInstance());
    }

    @Test
    void translate_success_returnsNull() {
        DataAccessException ex = FlexExceptionTranslator.getInstance()
                .translate("insert", () -> null);
        assertThat(ex).isNull();
    }

    @Test
    void translate_failure_returnsDataAccessExceptionWithSuppressed() {
        RuntimeException cause = new RuntimeException("boom");
        DataAccessException ex = FlexExceptionTranslator.getInstance()
                .translate("insert", () -> {
                    throw cause;
                });
        assertThat(ex).isInstanceOf(DataAccessException.class);
        assertThat(ex.getSuppressed()).contains(cause);
    }

    @Test
    void translate_nullAction_throws() {
        assertThatThrownBy(() -> FlexExceptionTranslator.getInstance().translate(null, () -> null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action must not be null");
    }

    @Test
    void translate_nullTask_throws() {
        assertThatThrownBy(() -> FlexExceptionTranslator.getInstance().translate("insert", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("task must not be null");
    }

    @Test
    void translate_static_withEntityClass_returnsDataAccessException() {
        DataAccessException ex = FlexExceptionTranslator.translate(
                "query", String.class, new RuntimeException("x"));
        assertThat(ex).isInstanceOf(DataAccessException.class);
    }

    @Test
    void translate_static_nullAction_throws() {
        assertThatThrownBy(() -> FlexExceptionTranslator.translate(null, String.class, new RuntimeException()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action must not be null");
    }

    @Test
    void translate_static_nullEx_throws() {
        assertThatThrownBy(() -> FlexExceptionTranslator.translate("query", String.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ex must not be null");
    }
}
