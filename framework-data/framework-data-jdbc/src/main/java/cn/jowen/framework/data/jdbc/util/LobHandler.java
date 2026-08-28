package cn.jowen.framework.data.jdbc.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * CLOB / BLOB 读写辅助工具。
 *
 * <p>封装大字段的常见读写，屏蔽不同 JDBC 驱动对 LOB 的细微差异（如 {@code setCharacterStream} 长度参数）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LobHandler {

    private LobHandler() {
    }

    /**
     * 将字符串以 CLOB 形式写入 PreparedStatement。
     *
     * @param stmt  PreparedStatement，不可为 {@code null}
     * @param index 参数位置（从 1 起）
     * @param value 字符串值，可为 {@code null}（写入 SQL NULL）
     * @throws SQLException 写入失败
     */
    public static void setClob(PreparedStatement stmt, int index, @Nullable String value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.CLOB);
            return;
        }
        Reader reader = new StringReader(value);
        stmt.setClob(index, reader, value.length());
    }

    /**
     * 以 CLOB 方式写入 PreparedStatement（接受 {@link Clob} 对象）。
     *
     * @param stmt  PreparedStatement，不可为 {@code null}
     * @param index 参数位置（从 1 起）
     * @param clob  Clob 对象，可为 {@code null}
     * @throws SQLException 写入失败
     */
    public static void setClob(PreparedStatement stmt, int index, @Nullable Clob clob) throws SQLException {
        if (clob == null) {
            stmt.setNull(index, Types.CLOB);
        } else {
            stmt.setClob(index, clob);
        }
    }

    /**
     * 从结果集按列名读取 CLOB 为字符串。
     *
     * @param rs         结果集，不可为 {@code null}
     * @param columnName 列名，不可为 {@code null}
     * @return 字符串，列值为 {@code null} 或空 CLOB 时返回 {@code null}
     * @throws SQLException 读取失败
     */
    @Nullable
    public static String getStringAsClob(ResultSet rs, String columnName) throws SQLException {
        Clob clob = rs.getClob(columnName);
        if (clob == null) {
            return null;
        }
        try (Reader reader = clob.getCharacterStream()) {
            if (reader == null) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new SQLException("Failed to read or close CLOB for column '" + columnName + "'", e);
        }
    }

    /**
     * 将字节数组以 BLOB 形式写入 PreparedStatement。
     *
     * @param stmt  PreparedStatement，不可为 {@code null}
     * @param index 参数位置（从 1 起）
     * @param value 字节数组，可为 {@code null}（写入 SQL NULL）
     * @throws SQLException 写入失败
     */
    public static void setBlob(PreparedStatement stmt, int index, @Nullable byte[] value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.BLOB);
            return;
        }
        InputStream in = new ByteArrayInputStream(value);
        stmt.setBlob(index, in, value.length);
    }

    /**
     * 以 BLOB 方式写入 PreparedStatement（接受 {@link Blob} 对象）。
     *
     * @param stmt  PreparedStatement，不可为 {@code null}
     * @param index 参数位置（从 1 起）
     * @param blob  Blob 对象，可为 {@code null}
     * @throws SQLException 写入失败
     */
    public static void setBlob(PreparedStatement stmt, int index, @Nullable Blob blob) throws SQLException {
        if (blob == null) {
            stmt.setNull(index, Types.BLOB);
        } else {
            stmt.setBlob(index, blob);
        }
    }

    /**
     * 从结果集按列名读取 BLOB 为字节数组。
     *
     * @param rs         结果集，不可为 {@code null}
     * @param columnName 列名，不可为 {@code null}
     * @return 字节数组，列为 {@code null} 时返回 {@code null}
     * @throws SQLException 读取失败
     */
    @Nullable
    public static byte[] getBytesAsBlob(ResultSet rs, String columnName) throws SQLException {
        Blob blob = rs.getBlob(columnName);
        if (blob == null) {
            return null;
        }
        try (InputStream in = blob.getBinaryStream()) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new SQLException("Failed to read or close BLOB for column '" + columnName + "'", e);
        }
    }
}
