package cn.jowen.framework.data.jdbc.core;

import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.mapping.RowMapper;
import cn.jowen.framework.data.core.mapping.TypeHandlerRegistry;
import cn.jowen.framework.data.jdbc.exception.SQLExceptionTranslator;
import cn.jowen.framework.data.jdbc.interceptor.InterceptorChain;
import cn.jowen.framework.data.jdbc.interceptor.SqlContext;
import cn.jowen.framework.data.jdbc.mapping.DefaultTypeHandlers;
import cn.jowen.framework.data.jdbc.mapping.ScalarRowMapper;
import cn.jowen.framework.data.jdbc.statement.ParameterBinder;
import cn.jowen.framework.data.jdbc.statement.PreparedStatementBuilder;
import cn.jowen.framework.data.jdbc.transaction.TransactionSynchronizationManager;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.util.JdbcUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * JDBC 核心模板：实现 {@link JdbcOperations}，封装连接获取、SQL 执行、结果集映射、异常翻译。
 *
 * <p>执行流程：
 * <ol>
 *   <li>取连接：事务内复用 {@link TransactionSynchronizationManager} 绑定的连接，否则从 {@link ConnectionProvider} 取新连接；</li>
 *   <li>经 {@link InterceptorChain} 责任链（日志/性能/多租户）；</li>
 *   <li>构建 {@link PreparedStatement} 并用 {@link ParameterBinder} 绑定参数；</li>
 *   <li>执行，将 {@link ResultSet} 转为 {@code List<Map<String, Object>>}；</li>
 *   <li>释放资源（事务内连接不关闭）；异常经 {@link SQLExceptionTranslator} 翻译。</li>
 * </ol>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class JdbcTemplate implements JdbcOperations {

    private final ConnectionProvider connectionProvider;
    private final InterceptorChain interceptorChain;
    private final TypeHandlerRegistry typeHandlerRegistry;
    private final SQLExceptionTranslator exceptionTranslator;

    public JdbcTemplate(ConnectionProvider connectionProvider, InterceptorChain interceptorChain) {
        this(connectionProvider, interceptorChain, DefaultTypeHandlers.getInstance(), new SQLExceptionTranslator());
    }

    public JdbcTemplate(ConnectionProvider connectionProvider, InterceptorChain interceptorChain,
                        TypeHandlerRegistry typeHandlerRegistry, SQLExceptionTranslator exceptionTranslator) {
        this.connectionProvider = connectionProvider;
        this.interceptorChain = interceptorChain;
        this.typeHandlerRegistry = typeHandlerRegistry;
        this.exceptionTranslator = exceptionTranslator;
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        List<Map<String, Object>> maps = queryForMaps(sql, args);
        List<T> result = new ArrayList<>(maps.size());
        for (int i = 0; i < maps.size(); i++) {
            T row = rowMapper.mapRow(maps.get(i), i);
            if (row != null) {
                result.add(row);
            }
        }
        return result;
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> list = query(sql, rowMapper, args);
        if (list.isEmpty()) {
            throw new DataAccessException("queryForObject 未返回任何行: " + sql);
        }
        return list.get(0);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
        return queryForObject(sql, new ScalarRowMapper<>(requiredType), args);
    }

    @Override
    public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
        List<Map<String, Object>> maps = queryForMaps(sql, args);
        ScalarRowMapper<T> mapper = new ScalarRowMapper<>(elementType);
        List<T> result = new ArrayList<>(maps.size());
        for (Map<String, Object> row : maps) {
            result.add(mapper.mapRow(row, 0));
        }
        return result;
    }

    @Override
    public int update(String sql, Object... args) {
        SqlContext ctx = new SqlContext(sql, toList(args));
        ctx.setKind(SqlContext.Kind.UPDATE);
        Object raw = interceptorChain.execute(() -> executeSql(ctx), ctx);
        return ((Number) raw).intValue();
    }

    @Override
    public void execute(String sql) {
        SqlContext ctx = new SqlContext(sql, List.of());
        ctx.setKind(SqlContext.Kind.EXECUTE);
        interceptorChain.execute(() -> executeSql(ctx), ctx);
    }

    @Override
    public int[] batchUpdate(String... sql) {
        boolean txBound = TransactionSynchronizationManager.getConnectionHolder() != null;
        Connection conn = txBound
                ? TransactionSynchronizationManager.getConnectionHolder().getConnection()
                : getConnection();
        try (Statement st = conn.createStatement()) {
            for (String s : sql) {
                st.addBatch(s);
            }
            return st.executeBatch();
        } catch (SQLException e) {
            throw exceptionTranslator.translate(e, "batchUpdate");
        } finally {
            if (!txBound) {
                JdbcUtils.closeQuietly(conn);
            }
        }
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
        boolean txBound = TransactionSynchronizationManager.getConnectionHolder() != null;
        Connection conn = txBound
                ? TransactionSynchronizationManager.getConnectionHolder().getConnection()
                : getConnection();
        try (PreparedStatement ps = PreparedStatementBuilder.create(conn, sql, List.of(), typeHandlerRegistry)) {
            for (Object[] params : batchArgs) {
                ParameterBinder.bindAll(ps, Arrays.asList(params), typeHandlerRegistry);
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw exceptionTranslator.translate(e, sql);
        } finally {
            if (!txBound) {
                JdbcUtils.closeQuietly(conn);
            }
        }
    }

    @Override
    public List<Map<String, Object>> queryForMaps(String sql, Object... args) {
        SqlContext ctx = new SqlContext(sql, toList(args));
        ctx.setKind(SqlContext.Kind.QUERY);
        Object raw = interceptorChain.execute(() -> executeSql(ctx), ctx);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> maps = (List<Map<String, Object>>) raw;
        return maps;
    }

    @Override
    public @Nullable Object executeInsert(String sql, Object... args) {
        SqlContext ctx = new SqlContext(sql, toList(args));
        ctx.setKind(SqlContext.Kind.INSERT);
        return interceptorChain.execute(() -> executeSql(ctx), ctx);
    }

    private Object executeSql(SqlContext ctx) {
        boolean txBound = TransactionSynchronizationManager.getConnectionHolder() != null;
        Connection conn = txBound
                ? TransactionSynchronizationManager.getConnectionHolder().getConnection()
                : getConnection();
        try {
            return switch (ctx.getKind()) {
                case QUERY -> {
                    try (PreparedStatement ps = PreparedStatementBuilder.create(conn, ctx.getSql(), ctx.getParams(),
                            typeHandlerRegistry)) {
                        try (ResultSet rs = ps.executeQuery()) {
                            yield JdbcUtils.resultSetToMaps(rs);
                        }
                    }
                }
                case INSERT -> {
                    try (PreparedStatement ps = PreparedStatementBuilder.create(conn, ctx.getSql(), ctx.getParams(),
                            typeHandlerRegistry, true)) {
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) {
                                yield keys.getObject(1);
                            }
                            yield null;
                        }
                    }
                }
                case UPDATE, BATCH -> {
                    try (PreparedStatement ps = PreparedStatementBuilder.create(conn, ctx.getSql(), ctx.getParams(),
                            typeHandlerRegistry)) {
                        yield ps.executeUpdate();
                    }
                }
                case EXECUTE -> {
                    try (Statement st = conn.createStatement()) {
                        st.execute(ctx.getSql());
                        yield null;
                    }
                }
            };
        } catch (SQLException e) {
            throw exceptionTranslator.translate(e, ctx.getSql());
        } finally {
            if (!txBound) {
                JdbcUtils.closeQuietly(conn);
            }
        }
    }

    private Connection getConnection() {
        try {
            return connectionProvider.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("获取数据库连接失败", e);
        }
    }

    private static List<Object> toList(Object[] args) {
        return new ArrayList<>(Arrays.asList(args));
    }
}
