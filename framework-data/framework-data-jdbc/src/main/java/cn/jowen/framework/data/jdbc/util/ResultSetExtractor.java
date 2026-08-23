package cn.jowen.framework.data.jdbc.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 结果集提取器：直接将 {@link ResultSet} 提取为对象列表（函数式）。
 *
 * <p>与 core 的 {@code RowMapper} 不同，本提取器直接操作底层 {@code ResultSet}，
 * 适用于需要流式/低层控制的场景（如大结果集分块读取）。
 *
 * @param <T> 提取结果元素类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
@FunctionalInterface
public interface ResultSetExtractor<T> {

    /**
     * 从结果集提取数据。
     *
     * @param rs 结果集，不可为 {@code null}（游标已停在首行之前，由实现决定遍历）
     * @return 提取出的对象列表，不可为 {@code null}
     * @throws SQLException 读取失败
     */
    List<T> extract(ResultSet rs) throws SQLException;

    /**
     * 提取单行首列标量值（便捷方法）。
     *
     * @param rs       结果集，不可为 {@code null}
     * @param type     目标类型，不可为 {@code null}
     * @param <T>      类型
     * @return 标量值，无数据时返回 {@code null}
     * @throws SQLException 读取失败
     */
    @Nullable
    static <T> T scalar(ResultSet rs, Class<T> type) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        Object value = rs.getObject(1);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new SQLException("无法将 " + value.getClass() + " 转换为 " + type);
    }
}
