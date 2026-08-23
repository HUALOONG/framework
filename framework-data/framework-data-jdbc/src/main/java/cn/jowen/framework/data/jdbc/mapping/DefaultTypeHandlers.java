package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.JdbcType;
import cn.jowen.framework.data.core.mapping.TypeHandler;
import cn.jowen.framework.data.core.mapping.TypeHandlerRegistry;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * 内置类型处理器注册中心（单例）。
 *
 * <p>注册 15+ 常用 Java 类型与 JDBC 列值之间的转换（含 {@code java.time} 类型与基本类型），
 * 通过 {@link #getInstance()} 获取全局唯一实例，供 {@link cn.jowen.framework.data.jdbc.statement.ParameterBinder} 使用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class DefaultTypeHandlers extends TypeHandlerRegistry {

    private static final DefaultTypeHandlers INSTANCE = new DefaultTypeHandlers();

    static {
        INSTANCE.registerAll();
    }

    private DefaultTypeHandlers() {
    }

    /**
     * 获取全局类型处理器注册中心单例。
     *
     * @return 单例，不可为 {@code null}
     */
    public static DefaultTypeHandlers getInstance() {
        return INSTANCE;
    }

    private void registerAll() {
        register(String.class, new StringHandler());
        register(Integer.class, new IntegerHandler());
        register(int.class, new IntegerHandler());
        register(Long.class, new LongHandler());
        register(long.class, new LongHandler());
        register(Boolean.class, new BooleanHandler());
        register(boolean.class, new BooleanHandler());
        register(BigDecimal.class, new BigDecimalHandler());
        register(Double.class, new DoubleHandler());
        register(double.class, new DoubleHandler());
        register(Float.class, new FloatHandler());
        register(float.class, new FloatHandler());
        register(Short.class, new ShortHandler());
        register(short.class, new ShortHandler());
        register(Byte.class, new ByteHandler());
        register(byte.class, new ByteHandler());
        register(Character.class, new CharacterHandler());
        register(char.class, new CharacterHandler());
        register(LocalDate.class, new LocalDateHandler());
        register(LocalDateTime.class, new LocalDateTimeHandler());
        register(LocalTime.class, new LocalTimeHandler());
        register(Date.class, new UtilDateHandler());
        register(java.sql.Date.class, new SqlDateHandler());
        register(java.sql.Time.class, new SqlTimeHandler());
        register(Timestamp.class, new SqlTimestampHandler());
        register(byte[].class, new ByteArrayHandler());
    }

    // ===================== 各类型处理器 =====================

    private abstract static class BaseHandler<T> implements TypeHandler<T> {
        @Override
        public final void setParameter(PreparedStatement stmt, int index, @Nullable T value, JdbcType jdbcType)
                throws SQLException {
            if (value == null) {
                stmt.setNull(index, jdbcType != null && jdbcType != JdbcType.OTHER ? jdbcType.getCode() : Types.NULL);
                return;
            }
            doSet(stmt, index, value);
        }

        protected abstract void doSet(PreparedStatement stmt, int index, T value) throws SQLException;

        public abstract @Nullable T getResult(ResultSet rs, int column) throws SQLException;

        @Override
        public @Nullable T getResult(ResultSet rs, String columnName) throws SQLException {
            return getResult(rs, rs.findColumn(columnName));
        }
    }

    private static final class StringHandler extends BaseHandler<String> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, String value) throws SQLException {
            stmt.setString(index, value);
        }

        @Override
        public @Nullable String getResult(ResultSet rs, int column) throws SQLException {
            return rs.getString(column);
        }
    }

    private static final class IntegerHandler extends BaseHandler<Integer> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Integer value) throws SQLException {
            stmt.setInt(index, value);
        }

        @Override
        public @Nullable Integer getResult(ResultSet rs, int column) throws SQLException {
            int v = rs.getInt(column);
            return rs.wasNull() ? null : v;
        }
    }

    private static final class LongHandler extends BaseHandler<Long> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Long value) throws SQLException {
            stmt.setLong(index, value);
        }

        @Override
        public @Nullable Long getResult(ResultSet rs, int column) throws SQLException {
            long v = rs.getLong(column);
            return rs.wasNull() ? null : v;
        }
    }

    private static final class BooleanHandler extends BaseHandler<Boolean> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Boolean value) throws SQLException {
            stmt.setBoolean(index, value);
        }

        @Override
        public @Nullable Boolean getResult(ResultSet rs, int column) throws SQLException {
            boolean v = rs.getBoolean(column);
            return rs.wasNull() ? null : v;
        }
    }

    private static final class BigDecimalHandler extends BaseHandler<BigDecimal> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, BigDecimal value) throws SQLException {
            stmt.setBigDecimal(index, value);
        }

        @Override
        public @Nullable BigDecimal getResult(ResultSet rs, int column) throws SQLException {
            return rs.getBigDecimal(column);
        }
    }

    private static final class DoubleHandler extends BaseHandler<Double> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Double value) throws SQLException {
            stmt.setDouble(index, value);
        }

        @Override
        public @Nullable Double getResult(ResultSet rs, int column) throws SQLException {
            double v = rs.getDouble(column);
            return rs.wasNull() ? null : v;
        }
    }

    private static final class FloatHandler extends BaseHandler<Float> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Float value) throws SQLException {
            stmt.setFloat(index, value);
        }

        @Override
        public @Nullable Float getResult(ResultSet rs, int column) throws SQLException {
            float v = rs.getFloat(column);
            return rs.wasNull() ? null : v;
        }
    }

    private static final class ShortHandler extends BaseHandler<Short> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Short value) throws SQLException {
            stmt.setShort(index, value);
        }

        @Override
        public @Nullable Short getResult(ResultSet rs, int column) throws SQLException {
            short v = rs.getShort(column);
            return rs.wasNull() ? null : v;
        }
    }

    private static final class ByteHandler extends BaseHandler<Byte> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Byte value) throws SQLException {
            stmt.setByte(index, value);
        }

        @Override
        public @Nullable Byte getResult(ResultSet rs, int column) throws SQLException {
            byte v = rs.getByte(column);
            return rs.wasNull() ? null : v;
        }
    }

    private static final class CharacterHandler extends BaseHandler<Character> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Character value) throws SQLException {
            stmt.setString(index, value.toString());
        }

        @Override
        public @Nullable Character getResult(ResultSet rs, int column) throws SQLException {
            String s = rs.getString(column);
            return (s == null || s.isEmpty()) ? null : s.charAt(0);
        }
    }

    private static final class LocalDateHandler extends BaseHandler<LocalDate> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, LocalDate value) throws SQLException {
            stmt.setDate(index, java.sql.Date.valueOf(value));
        }

        @Override
        public @Nullable LocalDate getResult(ResultSet rs, int column) throws SQLException {
            java.sql.Date d = rs.getDate(column);
            return d == null ? null : d.toLocalDate();
        }
    }

    private static final class LocalDateTimeHandler extends BaseHandler<LocalDateTime> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
            stmt.setTimestamp(index, Timestamp.valueOf(value));
        }

        @Override
        public @Nullable LocalDateTime getResult(ResultSet rs, int column) throws SQLException {
            Timestamp ts = rs.getTimestamp(column);
            return ts == null ? null : ts.toLocalDateTime();
        }
    }

    private static final class LocalTimeHandler extends BaseHandler<LocalTime> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, LocalTime value) throws SQLException {
            stmt.setTime(index, java.sql.Time.valueOf(value));
        }

        @Override
        public @Nullable LocalTime getResult(ResultSet rs, int column) throws SQLException {
            java.sql.Time t = rs.getTime(column);
            return t == null ? null : t.toLocalTime();
        }
    }

    private static final class UtilDateHandler extends BaseHandler<Date> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Date value) throws SQLException {
            stmt.setTimestamp(index, new Timestamp(value.getTime()));
        }

        @Override
        public @Nullable Date getResult(ResultSet rs, int column) throws SQLException {
            Timestamp ts = rs.getTimestamp(column);
            return ts == null ? null : new Date(ts.getTime());
        }
    }

    private static final class SqlDateHandler extends BaseHandler<java.sql.Date> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, java.sql.Date value) throws SQLException {
            stmt.setDate(index, value);
        }

        @Override
        public java.sql.Date getResult(ResultSet rs, int column) throws SQLException {
            return rs.getDate(column);
        }
    }

    private static final class SqlTimeHandler extends BaseHandler<java.sql.Time> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, java.sql.Time value) throws SQLException {
            stmt.setTime(index, value);
        }

        @Override
        public java.sql.Time getResult(ResultSet rs, int column) throws SQLException {
            return rs.getTime(column);
        }
    }

    private static final class SqlTimestampHandler extends BaseHandler<Timestamp> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, Timestamp value) throws SQLException {
            stmt.setTimestamp(index, value);
        }

        @Override
        public @Nullable Timestamp getResult(ResultSet rs, int column) throws SQLException {
            return rs.getTimestamp(column);
        }
    }

    private static final class ByteArrayHandler extends BaseHandler<byte[]> {
        @Override
        protected void doSet(PreparedStatement stmt, int index, byte[] value) throws SQLException {
            InputStream in = new ByteArrayInputStream(value);
            stmt.setBinaryStream(index, in, value.length);
        }

        @Override
        public @Nullable byte[] getResult(ResultSet rs, int column) throws SQLException {
            return rs.getBytes(column);
        }
    }

    /** 避免未使用的 Reader 导入告警（保留以备 CLOB 扩展）。 */
    @SuppressWarnings("unused")
    private static Reader unusedReader() {
        return new StringReader("");
    }
}
