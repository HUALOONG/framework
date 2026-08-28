package cn.jowen.framework.data.jdbc.exception;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.exception.DeadlockException;
import cn.jowen.framework.data.core.exception.DataIntegrityViolationException;
import cn.jowen.framework.data.core.exception.DuplicateKeyException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link VendorSpecificTranslator} 测试。
 */
class VendorSpecificTranslatorTest {

    private static SQLException ex(int code) {
        return new SQLException("boom", "00000", code);
    }

    @Test
    void mysql_uniqueViolation() {
        assertThat(VendorSpecificTranslator.translate(ex(1062), DatabaseType.MYSQL, "INSERT"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void mysql_deadlock() {
        assertThat(VendorSpecificTranslator.translate(ex(1205), DatabaseType.MYSQL, "UPDATE"))
                .isInstanceOf(DeadlockException.class);
        assertThat(VendorSpecificTranslator.translate(ex(1213), DatabaseType.MYSQL, "UPDATE"))
                .isInstanceOf(DeadlockException.class);
    }

    @Test
    void mysql_integrity() {
        assertThat(VendorSpecificTranslator.translate(ex(1451), DatabaseType.MYSQL, "DELETE"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(VendorSpecificTranslator.translate(ex(1452), DatabaseType.MYSQL, "INSERT"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(VendorSpecificTranslator.translate(ex(1364), DatabaseType.MYSQL, "INSERT"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oracle_codes() {
        assertThat(VendorSpecificTranslator.translate(ex(1), DatabaseType.ORACLE, "INSERT"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(VendorSpecificTranslator.translate(ex(60), DatabaseType.ORACLE, "UPDATE"))
                .isInstanceOf(DeadlockException.class);
        assertThat(VendorSpecificTranslator.translate(ex(4020), DatabaseType.ORACLE, "UPDATE"))
                .isInstanceOf(DeadlockException.class);
        assertThat(VendorSpecificTranslator.translate(ex(4021), DatabaseType.ORACLE, "UPDATE"))
                .isInstanceOf(DeadlockException.class);
    }

    @Test
    void postgresql_uniqueViolation() {
        assertThat(VendorSpecificTranslator.translate(ex(23505), DatabaseType.POSTGRESQL, "INSERT"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void unknownType_but1062_stillDuplicate() {
        assertThat(VendorSpecificTranslator.translate(ex(1062), DatabaseType.UNKNOWN, "INSERT"))
                .isInstanceOf(DuplicateKeyException.class);
        // null type 也走通用分支
        assertThat(VendorSpecificTranslator.translate(ex(1062), null, "INSERT"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void unrecognized_returnsNull() {
        assertThat(VendorSpecificTranslator.translate(ex(9999), DatabaseType.MYSQL, "SELECT"))
                .isNull();
        assertThat(VendorSpecificTranslator.translate(ex(0), null, "SELECT"))
                .isNull();
    }
}
