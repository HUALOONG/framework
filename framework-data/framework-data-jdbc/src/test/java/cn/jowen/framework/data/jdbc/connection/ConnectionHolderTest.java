package cn.jowen.framework.data.jdbc.connection;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link ConnectionHolder} 测试。
 */
class ConnectionHolderTest {

    private final Connection conn = mock(Connection.class);

    @Test
    void constructor_setsInitialState() {
        ConnectionHolder holder = new ConnectionHolder(conn, true);
        assertThat(holder.getConnection()).isSameAs(conn);
        assertThat(holder.isTransactionBound()).isTrue();
        assertThat(holder.getReferenceCount()).isEqualTo(1);
        assertThat(holder.isRollbackOnly()).isFalse();
    }

    @Test
    void incrementAndDecrement() {
        ConnectionHolder holder = new ConnectionHolder(conn, false);
        holder.increment();
        assertThat(holder.getReferenceCount()).isEqualTo(2);
        assertThat(holder.decrement()).isEqualTo(1);
        assertThat(holder.decrement()).isZero();
        // 不会降到 0 以下
        assertThat(holder.decrement()).isZero();
    }

    @Test
    void rollbackOnlyFlag() {
        ConnectionHolder holder = new ConnectionHolder(conn, false);
        holder.setRollbackOnly();
        assertThat(holder.isRollbackOnly()).isTrue();
    }

    @Test
    void toString_includesState() {
        ConnectionHolder holder = new ConnectionHolder(conn, true);
        assertThat(holder.toString()).contains("bound=true").contains("ref=1").contains("rollbackOnly=false");
    }
}
