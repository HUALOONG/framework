package cn.jowen.framework.data.core.mapping;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 类型处理器：在 JDBC 列值与 Java 类型之间做转换。实现层按需要注册到 {@link TypeHandlerRegistry}。
 *
 * @param <T> Java 类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface TypeHandler<T> {

    /**
     * 设置 PreparedStatement 参数。
     *
     * @param stmt  PreparedStatement，不可为 {@code null}
     * @param index 参数位置（从 1 起）
     * @param value 参数值，可为 {@code null}
     * @param jdbcType 目标 JDBC 类型
     * @throws Exception 设置失败时抛出
     */
    void setParameter(java.sql.PreparedStatement stmt, int index, @Nullable T value, JdbcType jdbcType) throws Exception;

    /**
     * 从 ResultSet 获取值。
     *
     * @param rs         ResultSet，不可为 {@code null}
     * @param columnName 列名，不可为 {@code null}
     * @return 转换后的值，可为 {@code null}
     * @throws Exception 读取失败时抛出
     */
    @Nullable T getResult(java.sql.ResultSet rs, String columnName) throws Exception;

    /**
     * 从 ResultSet 按索引获取值。
     *
     * @param rs     ResultSet，不可为 {@code null}
     * @param column Index（从 1 起）
     * @return 转换后的值，可为 {@code null}
     * @throws Exception 读取失败时抛出
     */
    @Nullable T getResult(java.sql.ResultSet rs, int column) throws Exception;
}
