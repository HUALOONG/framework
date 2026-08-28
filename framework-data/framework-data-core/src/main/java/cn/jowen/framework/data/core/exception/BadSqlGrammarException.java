package cn.jowen.framework.data.core.exception;

import org.jspecify.annotations.NullMarked;

/**
 * SQL 语法错误异常，当 SQL 语句存在语法错误或无法被数据库解析时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class BadSqlGrammarException extends DataAccessException {

    public BadSqlGrammarException(String message) {
        super(message);
    }

    public BadSqlGrammarException(String message, Throwable cause) {
        super(message, cause);
    }
}
