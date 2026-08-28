package cn.jowen.framework.data.jdbc.statement;

import cn.jowen.framework.data.core.mapping.TypeHandlerRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * 预编译语句构建器：根据 {@link SqlResult} 创建并填充 {@link PreparedStatement}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class PreparedStatementBuilder {

    private PreparedStatementBuilder() {
    }

    /**
     * 创建并填充一条预编译语句（不含生成键）。
     *
     * @param connection 连接，不可为 {@code null}
     * @param sqlResult  SQL 与参数，不可为 {@code null}
     * @param registry   类型处理器注册中心，不可为 {@code null}
     * @return 已绑定参数的 PreparedStatement
     * @throws SQLException 构建失败
     */
    public static PreparedStatement create(Connection connection, SqlResult sqlResult,
                                            TypeHandlerRegistry registry) throws SQLException {
        return create(connection, sqlResult, registry, false);
    }

    /**
     * 创建并填充一条预编译语句。
     *
     * @param connection           连接，不可为 {@code null}
     * @param sqlResult            SQL 与参数，不可为 {@code null}
     * @param registry             类型处理器注册中心，不可为 {@code null}
     * @param returnGeneratedKeys 是否请求返回自增主键
     * @return 已绑定参数的 PreparedStatement
     * @throws SQLException 构建失败
     */
    public static PreparedStatement create(Connection connection, SqlResult sqlResult,
                                            TypeHandlerRegistry registry,
                                            boolean returnGeneratedKeys) throws SQLException {
        PreparedStatement stmt = returnGeneratedKeys
                ? connection.prepareStatement(sqlResult.sql(), java.sql.Statement.RETURN_GENERATED_KEYS)
                : connection.prepareStatement(sqlResult.sql());
        ParameterBinder.bindAll(stmt, sqlResult.params(), registry);
        return stmt;
    }

    /**
     * 创建并填充一条预编译语句（直接接收 SQL 与参数列表）。
     *
     * @param connection 连接，不可为 {@code null}
     * @param sql       SQL 文本，不可为 {@code null}
     * @param params    参数列表，不可为 {@code null}
     * @param registry  类型处理器注册中心，不可为 {@code null}
     * @return 已绑定参数的 PreparedStatement
     * @throws SQLException 构建失败
     */
    public static PreparedStatement create(Connection connection, String sql, List<@Nullable Object> params,
                                            TypeHandlerRegistry registry) throws SQLException {
        return create(connection, new SqlResult(sql, params), registry, false);
    }

    /**
     * 创建并填充一条预编译语句（直接接收 SQL 与参数列表，可选生成键）。
     *
     * @param connection           连接，不可为 {@code null}
     * @param sql                  SQL 文本，不可为 {@code null}
     * @param params               参数列表，不可为 {@code null}
     * @param registry             类型处理器注册中心，不可为 {@code null}
     * @param returnGeneratedKeys 是否请求返回自增主键
     * @return 已绑定参数的 PreparedStatement
     * @throws SQLException 构建失败
     */
    public static PreparedStatement create(Connection connection, String sql, List<@Nullable Object> params,
                                            TypeHandlerRegistry registry,
                                            boolean returnGeneratedKeys) throws SQLException {
        return create(connection, new SqlResult(sql, params), registry, returnGeneratedKeys);
    }
}
