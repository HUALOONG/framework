package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.core.transaction.Isolation;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link IsolationLevelManager} 单元测试。
 */
class IsolationLevelManagerTest {

    @Test
    void toJdbc_defaultMapsToRepeatableRead() {
        assertThat(IsolationLevelManager.toJdbc(Isolation.DEFAULT))
                .isEqualTo(Connection.TRANSACTION_REPEATABLE_READ);
    }

    @Test
    void toJdbc_readUncommitted() {
        assertThat(IsolationLevelManager.toJdbc(Isolation.READ_UNCOMMITTED))
                .isEqualTo(Connection.TRANSACTION_READ_UNCOMMITTED);
    }

    @Test
    void toJdbc_readCommitted() {
        assertThat(IsolationLevelManager.toJdbc(Isolation.READ_COMMITTED))
                .isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
    }

    @Test
    void toJdbc_repeatableRead() {
        assertThat(IsolationLevelManager.toJdbc(Isolation.REPEATABLE_READ))
                .isEqualTo(Connection.TRANSACTION_REPEATABLE_READ);
    }

    @Test
    void toJdbc_serializable() {
        assertThat(IsolationLevelManager.toJdbc(Isolation.SERIALIZABLE))
                .isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
    }
}
