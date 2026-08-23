package cn.jowen.framework.data.jdbc.connection;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.function.Consumer;

/**
 * 连接代理：包装真实 {@link Connection}，拦截 {@code close()}。
 *
 * <p>事务内连接不应被调用方（如 {@code JdbcTemplate} 的 finally 分支）真正关闭，
 * 而是委托给 {@code onClose} 回调（通常由事务管理器统一回收真实连接）。
 * 其余方法全部透传到真实连接。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class ConnectionProxy {

    private ConnectionProxy() {
    }

    /**
     * 创建代理连接。
     *
     * @param real    真实连接，不可为 {@code null}
     * @param onClose 关闭回调，不可为 {@code null}
     * @return 代理连接
     */
    public static Connection wrap(Connection real, Consumer<? super Connection> onClose) {
        return (Connection) Proxy.newProxyInstance(
                ConnectionProxy.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new Handler(real, onClose));
    }

    private static final class Handler implements InvocationHandler {
        private final Connection real;
        private final Consumer<? super Connection> onClose;
        private boolean closed = false;

        Handler(Connection real, Consumer<? super Connection> onClose) {
            this.real = real;
            this.onClose = onClose;
        }

        @Override
        public @Nullable Object invoke(Object proxy, Method method, Object @Nullable [] args) throws Throwable {
            if ("close".equals(method.getName())) {
                synchronized (this) {
                    if (!closed) {
                        closed = true;
                        onClose.accept(real);
                    }
                }
                return null;
            }
            if ("isClosed".equals(method.getName())) {
                return closed || real.isClosed();
            }
            try {
                return method.invoke(real, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
