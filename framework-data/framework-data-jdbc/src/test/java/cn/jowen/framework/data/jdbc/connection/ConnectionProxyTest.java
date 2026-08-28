package cn.jowen.framework.data.jdbc.connection;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ConnectionProxy} 测试。
 */
class ConnectionProxyTest {

    @Test
    void close_delegatesToCallbackAndIsIdempotent() throws Exception {
        Connection real = mock(Connection.class);
        AtomicInteger calls = new AtomicInteger();
        Connection proxy = ConnectionProxy.wrap(real, c -> calls.incrementAndGet());

        proxy.close();
        proxy.close();
        proxy.close();

        assertThat(calls.get()).isEqualTo(1);
        // 真实连接不被代理 close
        verify(real, never()).close();
    }

    @Test
    void isClosed_reflectsClosedState() throws Exception {
        Connection real = mock(Connection.class);
        when(real.isClosed()).thenReturn(false);
        Connection proxy = ConnectionProxy.wrap(real, c -> {
        });

        assertThat(proxy.isClosed()).isFalse();
        proxy.close();
        assertThat(proxy.isClosed()).isTrue();
    }

    @Test
    void otherMethods_passThrough() throws Exception {
        Connection real = mock(Connection.class);
        when(real.getCatalog()).thenReturn("cat");
        Connection proxy = ConnectionProxy.wrap(real, c -> {
        });

        assertThat(proxy.getCatalog()).isEqualTo("cat");
        verify(real).getCatalog();
    }
}
