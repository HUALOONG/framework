package cn.jowen.framework.data.jdbc.exception;

import cn.jowen.framework.data.core.exception.BadSqlGrammarException;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.exception.DataIntegrityViolationException;
import cn.jowen.framework.data.core.exception.DeadlockException;
import cn.jowen.framework.data.core.exception.DuplicateKeyException;
import cn.jowen.framework.data.core.exception.TransientDataAccessException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;

/**
 * SQLState 分类器：依据 {@link SQLException#getSQLState()} 将异常映射到具体的 {@link DataAccessException} 子类。
 *
 * <p>采用模式匹配 {@code switch} 覆盖常见 SQLState 类别（连接/完整性/事务回滚/语法等）。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class SqlStateClassifier {

    private SqlStateClassifier() {
    }

    /**
     * 根据 SQLState 分类异常。
     *
     * @param ex  SQL 异常，不可为 {@code null}
     * @param sql 触发异常的 SQL（用于异常信息），不可为 {@code null}
     * @return 框架统一数据访问异常，不可为 {@code null}
     */
    public static DataAccessException classify(SQLException ex, String sql) {
        String state = ex.getSQLState();
        if (state == null) {
            return new DataAccessException("SQL 执行失败: " + sql, ex);
        }
        String message = "SQL=[" + sql + "] SQLState=[" + state + "]";
        return switch (state) {
            // 唯一约束冲突（H2/PostgreSQL/MySQL 通用）
            case "23505", "23503", "23514", "23502" -> new DataIntegrityViolationException(message, ex);
            case "23000", "23001", "23900" -> new DataIntegrityViolationException(message, ex);
            // 事务回滚
            case "40001" -> new DeadlockException(message, ex); // 序列化失败
            case "40002", "40003", "40P01" -> new TransientDataAccessException(message, ex);
            case "40" -> new TransientDataAccessException(message, ex);
            // 连接/通信失败（瞬态）
            case "08" -> new TransientDataAccessException(message, ex);
            case "08000", "08003", "08006", "08001", "08004", "08007" -> new TransientDataAccessException(message, ex);
            // 语法/访问规则错误
            case "42", "42000", "42001", "42601", "HY000", "HY001", "HY009", "HYC00", "HYT00", "HYT01" ->
                    new BadSqlGrammarException(message, ex);
            // 超时
            case "S1T00", "57014" -> new cn.jowen.framework.data.core.exception.TimeoutException(message, ex);
            default -> {
                if (state.startsWith("23")) {
                    yield new DataIntegrityViolationException(message, ex);
                }
                if (state.startsWith("42") || state.startsWith("HY")) {
                    yield new BadSqlGrammarException(message, ex);
                }
                if (state.startsWith("40")) {
                    yield new TransientDataAccessException(message, ex);
                }
                yield new DataAccessException(message, ex);
            }
        };
    }

    @Nullable
    private static DataAccessException passThrough() {
        return null;
    }
}
