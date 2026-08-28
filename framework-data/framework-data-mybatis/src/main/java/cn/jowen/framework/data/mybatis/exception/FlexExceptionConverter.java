package cn.jowen.framework.data.mybatis.exception;

import cn.jowen.framework.data.core.exception.BadSqlGrammarException;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.exception.DataIntegrityViolationException;
import cn.jowen.framework.data.core.exception.DuplicateKeyException;
import cn.jowen.framework.data.core.exception.ExceptionTranslator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * MyBatis 异常 → 框架统一异常体系转换。
 *
 * <p>转换规则：
 * <ul>
 *   <li>SQL 语法错误 → {@link BadSqlGrammarException}</li>
 *   <li>唯一约束冲突 → {@link DuplicateKeyException}</li>
 *   <li>其他约束违反 → {@link DataIntegrityViolationException}</li>
 *   <li>乐观锁冲突 → {@link FlexOptimisticLockException}</li>
 *   <li>其他 → {@link DataAccessException}</li>
 * </ul>
 * </p>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class FlexExceptionConverter {

    public static final FlexExceptionConverter INSTANCE = new FlexExceptionConverter();

    private FlexExceptionConverter() { }

    public DataAccessException translate(@Nullable Throwable ex) {
        if (ex == null) {
            return new DataAccessException("未知异常");
        }
        String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
        String lowerMsg = msg.toLowerCase();

        // 乐观锁冲突：影响行数为 0
        if (lowerMsg.contains("optimistic") || lowerMsg.contains("version")) {
            return new FlexOptimisticLockException(ex.getMessage());
        }

        // SQL 语法
        if (lowerMsg.contains("sql syntax") || lowerMsg.contains("sql grammar") || lowerMsg.contains("bad sql")) {
            return new BadSqlGrammarException(msg, ex);
        }

        // 唯一约束冲突
        if (lowerMsg.contains("unique") || lowerMsg.contains("duplicate") || lowerMsg.contains("1062")) {
            return new DuplicateKeyException(msg, ex);
        }

        // 其他约束违反
        if (lowerMsg.contains("constraint") || lowerMsg.contains("integrity")) {
            return new DataIntegrityViolationException(msg, ex);
        }

        return new DataAccessException(msg, ex);
    }
}