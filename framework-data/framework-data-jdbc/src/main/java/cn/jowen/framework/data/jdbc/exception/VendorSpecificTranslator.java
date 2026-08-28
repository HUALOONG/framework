package cn.jowen.framework.data.jdbc.exception;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.exception.DeadlockException;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.exception.DataIntegrityViolationException;
import cn.jowen.framework.data.core.exception.DuplicateKeyException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;

/**
 * 厂商特定错误码翻译器：在 {@link SqlStateClassifier} 基础上，按数据库厂商错误码补充更精确的映射。
 *
 * <p>目前覆盖 MySQL（1062 唯一键冲突、1205/1213 死锁、1451/1452 外键完整性）与 Oracle（1 唯一键冲突、60 死锁）。
 * 无法识别时返回 {@code null}，交由 {@link SQLExceptionTranslator} 回退到 SQLState 分类。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class VendorSpecificTranslator {

    private VendorSpecificTranslator() {
    }

    /**
     * 按厂商错误码翻译异常。
     *
     * @param ex     SQL 异常，不可为 {@code null}
     * @param type   数据库类型，可为 {@code null}（不确定时按通用规则）
     * @param sql    触发 SQL，不可为 {@code null}
     * @return 翻译后的异常；无法识别返回 {@code null}
     */
    public static @Nullable DataAccessException translate(SQLException ex, @Nullable DatabaseType type, String sql) {
        int code = ex.getErrorCode();
        String message = "SQL=[" + sql + "] vendorErrorCode=[" + code + "]";
        if (type == DatabaseType.MYSQL) {
            if (code == 1062) {
                return new DuplicateKeyException(message, ex);
            }
            if (code == 1205 || code == 1213) {
                return new DeadlockException(message, ex);
            }
            if (code == 1451 || code == 1452 || code == 1364) {
                return new DataIntegrityViolationException(message, ex);
            }
        } else if (type == DatabaseType.ORACLE) {
            if (code == 1) {
                return new DuplicateKeyException(message, ex);
            }
            if (code == 60 || code == 4020 || code == 4021) {
                return new DeadlockException(message, ex);
            }
        } else if (type == DatabaseType.POSTGRESQL) {
            if (code == 23505) {
                return new DuplicateKeyException(message, ex);
            }
        }
        // 通用：MySQL 1062 即使 type 未知也强烈暗示唯一冲突
        if (code == 1062) {
            return new DuplicateKeyException(message, ex);
        }
        return null;
    }
}
