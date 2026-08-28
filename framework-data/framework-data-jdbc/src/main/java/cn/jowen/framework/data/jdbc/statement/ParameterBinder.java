package cn.jowen.framework.data.jdbc.statement;

import cn.jowen.framework.data.core.mapping.JdbcType;
import cn.jowen.framework.data.core.mapping.TypeHandler;
import cn.jowen.framework.data.core.mapping.TypeHandlerRegistry;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 参数绑定器：将 {@code Object[]} / {@code List<Object>} 绑定到 {@link PreparedStatement}。
 *
 * <p>绑定策略：
 * <ul>
 *   <li>{@code null} 值：调用 {@link PreparedStatement#setNull(int, int)}，sqlType 取 {@link Types#NULL}；</li>
 *   <li>非 {@code null} 值：优先查找 {@link TypeHandlerRegistry} 中按值类型注册的 {@link TypeHandler}；
 *       找不到则回退到 {@link PreparedStatement#setObject(int, Object)}。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class ParameterBinder {

    private ParameterBinder() {
    }

    /**
     * 绑定单个参数。
     *
     * @param stmt      PreparedStatement，不可为 {@code null}
     * @param index     参数位置（从 1 起）
     * @param value     参数值，可为 {@code null}
     * @param registry  类型处理器注册中心，不可为 {@code null}
     * @throws SQLException 绑定失败
     */
    public static void bind(PreparedStatement stmt, int index, @Nullable Object value,
                            TypeHandlerRegistry registry) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
            return;
        }
        TypeHandler<?> handler = registry.get(value);
        if (handler != null) {
            try {
                @SuppressWarnings("unchecked")
                TypeHandler<Object> typed = (TypeHandler<Object>) handler;
                typed.setParameter(stmt, index, value, JdbcType.OTHER);
            } catch (Exception e) {
                throw new SQLException("参数绑定失败，index=" + index, e);
            }
            return;
        }
        stmt.setObject(index, value);
    }

    /**
     * 批量绑定一组参数。
     *
     * @param stmt     PreparedStatement，不可为 {@code null}
     * @param params   参数列表，不可为 {@code null}
     * @param registry 类型处理器注册中心，不可为 {@code null}
     * @throws SQLException 绑定失败
     */
    public static void bindAll(PreparedStatement stmt, java.util.List<?> params,
                               TypeHandlerRegistry registry) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            bind(stmt, i + 1, params.get(i), registry);
        }
    }
}
