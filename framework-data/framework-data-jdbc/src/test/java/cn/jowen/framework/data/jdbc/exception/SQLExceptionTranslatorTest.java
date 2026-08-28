package cn.jowen.framework.data.jdbc.exception;

import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.exception.DuplicateKeyException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SQLExceptionTranslator} 单元测试。
 */
class SQLExceptionTranslatorTest {

    private final SQLExceptionTranslator translator = new SQLExceptionTranslator();

    @Test
    void translate_sqlException_vendorFirst() {
        DataAccessException ex = translator.translate(new SQLException("dup", "23000", 1062), "INSERT");
        assertThat(ex).isInstanceOf(DuplicateKeyException.class);
        assertThat(ex).hasMessageContaining("INSERT");
    }

    @Test
    void translate_sqlException_fallsBackToSqlState() {
        DataAccessException ex = translator.translate(new SQLException("bad", "S1000", 0), "SELECT");
        assertThat(ex).isInstanceOf(DataAccessException.class);
    }

    @Test
    void translate_action_taskSuccess_returnsNull() throws Exception {
        assertThat(translator.translate("op", () -> null)).isNull();
    }

    @Test
    void translate_action_sqlException_maps() throws Exception {
        DataAccessException ex = translator.translate("INSERT",
                () -> { throw new SQLException("dup", "23000", 1062); });
        assertThat(ex).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void translate_action_runtimeException_rethrown() {
        IllegalStateException boom = new IllegalStateException("boom");
        assertThatThrownBy(() -> translator.translate("op",
                () -> { throw boom; })).isSameAs(boom);
    }

    @Test
    void translate_action_checkedException_wrapped() {
        assertThatThrownBy(() -> translator.translate("op",
                () -> { throw new java.io.IOException("io"); }))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("op");
    }
}
