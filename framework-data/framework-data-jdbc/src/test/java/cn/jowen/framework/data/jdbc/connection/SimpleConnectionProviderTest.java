package cn.jowen.framework.data.jdbc.connection;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SimpleConnectionProvider} 单元测试（H2 内存库）。
 */
class SimpleConnectionProviderTest {

    private DataSourceProperties h2Properties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:h2:mem:simple_provider;DB_CLOSE_DELAY=-1");
        properties.setUsername("sa");
        properties.setDriverClassName("org.h2.Driver");
        return properties;
    }

    @Test
    void getConnection_returnsConnection() throws Exception {
        SimpleConnectionProvider provider = new SimpleConnectionProvider(h2Properties());
        try (Connection connection = provider.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
        } finally {
            provider.close();
        }
    }

    @Test
    void close_closesOpenedConnections() throws Exception {
        SimpleConnectionProvider provider = new SimpleConnectionProvider(h2Properties());
        Connection connection = provider.getConnection();
        assertThat(provider.isClosed()).isFalse();

        provider.close();

        assertThat(provider.isClosed()).isTrue();
        assertThat(connection.isClosed()).isTrue();
    }

    @Test
    void close_isIdempotent() {
        SimpleConnectionProvider provider = new SimpleConnectionProvider(h2Properties());
        provider.close();
        assertThatCode(provider::close).doesNotThrowAnyException();
    }

    @Test
    void getConnection_afterClose_throws() throws Exception {
        SimpleConnectionProvider provider = new SimpleConnectionProvider(h2Properties());
        provider.close();

        assertThatThrownBy(provider::getConnection)
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("已关闭");
    }

    @Test
    void constructor_driverNotFound_throws() {
        DataSourceProperties properties = h2Properties();
        properties.setDriverClassName("com.example.NoSuchDriver");

        assertThatThrownBy(() -> new SimpleConnectionProvider(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法加载数据库驱动");
    }

    @Test
    void getConnection_invalidUrl_throws() {
        DataSourceProperties properties = h2Properties();
        properties.setUrl("jdbc:invalid:url");

        SimpleConnectionProvider provider = new SimpleConnectionProvider(properties);

        assertThatThrownBy(provider::getConnection)
                .isInstanceOf(SQLException.class);
    }

    @Test
    void constructor_noDriverClass_ok() {
        DataSourceProperties properties = h2Properties();
        properties.setDriverClassName(null);
        // H2 驱动已在 classpath，DriverManager 可自动加载
        SimpleConnectionProvider provider = new SimpleConnectionProvider(properties);
        assertThatCode(provider::close).doesNotThrowAnyException();
    }
}
