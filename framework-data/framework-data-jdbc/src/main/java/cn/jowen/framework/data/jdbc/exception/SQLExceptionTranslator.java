package cn.jowen.framework.data.jdbc.exception;

import cn.jowen.framework.data.core.exception.ExceptionTranslator;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.core.spi.SPIImplementation;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * SQLException 翻译器：将 JDBC {@link SQLException} 翻译为框架统一的 {@link DataAccessException} 体系。
 *
 * <p>优先使用 {@link VendorSpecificTranslator} 按错误码精确映射，未命中则回退到 {@link SqlStateClassifier}。
 * 同时实现 core 的 {@link ExceptionTranslator#translate(String, Callable)} 以兼容统一异常翻译入口。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@SPIImplementation(name = "jdbc")
public final class SQLExceptionTranslator implements ExceptionTranslator {

    /**
     * 将 SQLException 翻译为数据访问异常。
     *
     * @param ex  SQL 异常，不可为 {@code null}
     * @param sql 触发异常的 SQL，不可为 {@code null}
     * @return 框架统一异常，不可为 {@code null}
     */
    public DataAccessException translate(SQLException ex, String sql) {
        DataAccessException byVendor = VendorSpecificTranslator.translate(ex, null, sql);
        if (byVendor != null) {
            return byVendor;
        }
        return SqlStateClassifier.classify(ex, sql);
    }

    /**
     * 将指定操作的底层异常翻译为框架数据访问异常（兼容 core {@link ExceptionTranslator}）。
     *
     * @param action 操作描述，不可为 {@code null}
     * @param task   执行任务，不可为 {@code null}
     * @return 翻译后的异常；任务未抛异常返回 {@code null}
     */
    @Override
    public @Nullable DataAccessException translate(String action, Callable<Void> task) {
        try {
            task.call();
            return null;
        } catch (SQLException e) {
            return translate(e, action);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DataAccessException(action, e);
        }
    }
}
