package cn.jowen.framework.data.jdbc.connection;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DataSourceConnectionProvider} 单元测试。
 */
class DataSourceConnectionProviderTest {

    @Test
    void getConnection_delegatesToDataSource() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        DataSourceConnectionProvider provider = new DataSourceConnectionProvider(dataSource);

        assertThat(provider.getConnection()).isSameAs(connection);
        verify(dataSource).getConnection();
    }

    @Test
    void getConnection_propagatesSqlException() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("boom"));

        DataSourceConnectionProvider provider = new DataSourceConnectionProvider(dataSource);

        assertThatThrownBy(provider::getConnection)
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void close_isNoOp() {
        DataSource dataSource = mock(DataSource.class);
        DataSourceConnectionProvider provider = new DataSourceConnectionProvider(dataSource);

        provider.close();

        assertThat(provider.isClosed()).isFalse();
    }

    @Test
    void isClosed_alwaysFalse() {
        DataSourceConnectionProvider provider =
                new DataSourceConnectionProvider(mock(DataSource.class));
        assertThat(provider.isClosed()).isFalse();
    }
}
