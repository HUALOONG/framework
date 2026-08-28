package cn.jowen.framework.data.jdbc.util;

import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 工具类，承载资源关闭与结果集转换等通用操作。
 *
 * <p>内部流程统一约定：{@link java.sql.ResultSet} 先转换为 {@code List<Map<String, Object>>}
 * （列名 → 值），再交由 {@code RowMapper} 完成对象映射，从而与 core 抽象层解耦。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class JdbcUtils {

    private JdbcUtils() {
    }

    /**
     * 安静关闭连接（忽略异常）。
     *
     * @param connection 连接，可为 {@code null}
     */
    public static void closeQuietly(@Nullable Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // 静默忽略
        }
    }

    /**
     * 安静关闭语句（忽略异常）。
     *
     * @param statement 语句，可为 {@code null}
     */
    public static void closeQuietly(@Nullable Statement statement) {
        if (statement == null) {
            return;
        }
        try {
            statement.close();
        } catch (SQLException ignored) {
            // 静默忽略
        }
    }

    /**
     * 安静关闭结果集（忽略异常）。
     *
     * @param resultSet 结果集，可为 {@code null}
     */
    public static void closeQuietly(@Nullable ResultSet resultSet) {
        if (resultSet == null) {
            return;
        }
        try {
            resultSet.close();
        } catch (SQLException ignored) {
            // 静默忽略
        }
    }

    /**
     * 将 {@link ResultSet} 整表读取为「列名 → 值」的 Map 列表（保留列顺序）。
     *
     * <p>按列标签（column label）取列名；若出现重复标签，后者覆盖前者（调用方应避免 SELECT 重复列）。
     *
     * @param resultSet 结果集，不可为 {@code null}
     * @return 行 Map 列表，不可为 {@code null}
     * @throws SQLException 读取失败
     */
    public static List<Map<String, Object>> resultSetToMaps(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                String label = metaData.getColumnLabel(i);
                if (label == null || label.isEmpty()) {
                    label = metaData.getColumnName(i);
                }
                row.put(label.toLowerCase(), resultSet.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * 读取结果集的列名列表（按列顺序）。
     *
     * @param resultSet 结果集，不可为 {@code null}
     * @return 列名列表，不可为 {@code null}
     * @throws SQLException 读取失败
     */
    public static List<String> getColumnNames(ResultSet resultSet) throws SQLException {
        List<String> names = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String label = metaData.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = metaData.getColumnName(i);
            }
            names.add(label);
        }
        return names;
    }
}
