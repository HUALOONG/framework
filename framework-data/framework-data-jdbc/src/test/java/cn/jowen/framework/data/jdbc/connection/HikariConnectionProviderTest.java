package cn.jowen.framework.data.jdbc.connection;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link HikariConnectionProvider} 单元测试（H2 内存库）。
 */
class HikariConnectionProviderTest {

    private DataSourceProperties h2Properties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:h2:mem:hikari_provider;DB_CLOSE_DELAY=-1;USER=sa;PASSWORD=");
        properties.setUsername("sa");
        properties.setPassword("");
        properties.setDriverClassName("org.h2.Driver");
        properties.setName("test");
        properties.setMaximumPoolSize(5);
        properties.setConnectionTimeout(3000);
        properties.setIdleTimeout(60000);
        properties.setMaxLifetime(300000);
        return properties;
    }

    @Test
    void getConnection_returnsConnection() throws Exception {
        HikariConnectionProvider provider = new HikariConnectionProvider(h2Properties());
        try (Connection connection = provider.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
        } finally {
            provider.close();
        }
    }

    @Test
    void close_thenIsClosed() throws Exception {
        HikariConnectionProvider provider = new HikariConnectionProvider(h2Properties());
        assertThat(provider.isClosed()).isFalse();

        provider.close();

        assertThat(provider.isClosed()).isTrue();
    }

    @Test
    void close_isIdempotent() {
        HikariConnectionProvider provider = new HikariConnectionProvider(h2Properties());
        provider.close();
        provider.close();
        assertThat(provider.isClosed()).isTrue();
    }

    @Test
    void getConnection_invalidUrl_throws() {
        DataSourceProperties properties = h2Properties();
        properties.setUrl("jdbc:invalid:url");
        properties.setConnectionTimeout(1000);

        HikariConnectionProvider provider = new HikariConnectionProvider(properties);

        assertThatThrownBy(provider::getConnection)
                .isInstanceOf(RuntimeException.class);
        provider.close();
    }

    @Test
    void constructor_invalidDriverClass_throws() {
        DataSourceProperties properties = h2Properties();
        properties.setDriverClassName("com.example.NoSuchDriver");

        assertThatThrownBy(() -> new HikariConnectionProvider(properties))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructor_withoutUsernameAndDriver_ok() throws Exception {
        DataSourceProperties properties = h2Properties();
        properties.setUsername(null);
        properties.setPassword(null);
        properties.setDriverClassName(null);

        HikariConnectionProvider provider = new HikariConnectionProvider(properties);
        try (Connection connection = provider.getConnection()) {
            assertThat(connection).isNotNull();
        } finally {
            provider.close();
        }
    }
}
