package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.JdbcType;
import cn.jowen.framework.data.core.mapping.TypeHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultTypeHandlers} 单元测试。
 */
class DefaultTypeHandlersTest {

    private final ResultSet rs = mock(ResultSet.class);
    private final PreparedStatement ps = mock(PreparedStatement.class);

    // ---------- 基础行为 ----------

    @Test
    void getInstance_isSingleton() {
        assertThat(DefaultTypeHandlers.getInstance()).isSameAs(DefaultTypeHandlers.getInstance());
    }

    @Test
    void getHandler_registeredTypes() {
        assertThat(DefaultTypeHandlers.getInstance().get(String.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Integer.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(int.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Long.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Boolean.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(BigDecimal.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Double.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Float.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Short.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Byte.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Character.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(char.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(LocalDate.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(LocalDateTime.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(LocalTime.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(java.util.Date.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Date.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Time.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Timestamp.class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(byte[].class)).isNotNull();
        assertThat(DefaultTypeHandlers.getInstance().get(Object.class)).isNull();
    }

    // ---------- String ----------

    @Test
    void stringHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(String.class)
                .setParameter(ps, 1, "hello", JdbcType.VARCHAR);
        verify(ps).setString(1, "hello");
    }

    @Test
    void stringHandler_setNull_whenValueNull() throws Exception {
        DefaultTypeHandlers.getInstance().get(String.class)
                .setParameter(ps, 1, null, JdbcType.VARCHAR);
        verify(ps).setNull(1, Types.VARCHAR);
    }

    @Test
    void stringHandler_setNull_whenJdbcTypeNull() throws Exception {
        DefaultTypeHandlers.getInstance().get(String.class)
                .setParameter(ps, 1, null, null);
        verify(ps).setNull(1, Types.NULL);
    }

    @Test
    void stringHandler_setNull_whenJdbcTypeOther() throws Exception {
        DefaultTypeHandlers.getInstance().get(String.class)
                .setParameter(ps, 1, null, JdbcType.OTHER);
        verify(ps).setNull(1, Types.NULL);
    }

    @Test
    void stringHandler_getResult_byIndex() throws Exception {
        when(rs.getString(1)).thenReturn("abc");
        String value = DefaultTypeHandlers.getInstance().get(String.class).getResult(rs, 1);
        assertThat(value).isEqualTo("abc");
    }

    @Test
    void stringHandler_getResult_byColumnName() throws Exception {
        when(rs.findColumn("name")).thenReturn(2);
        when(rs.getString(2)).thenReturn("xyz");
        String value = DefaultTypeHandlers.getInstance().get(String.class).getResult(rs, "name");
        assertThat(value).isEqualTo("xyz");
    }

    // ---------- Integer ----------

    @Test
    void integerHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(Integer.class)
                .setParameter(ps, 1, 42, JdbcType.INTEGER);
        verify(ps).setInt(1, 42);
    }

    @Test
    void integerHandler_getResult() throws Exception {
        when(rs.getInt(1)).thenReturn(7);
        when(rs.wasNull()).thenReturn(false);
        Integer value = DefaultTypeHandlers.getInstance().get(Integer.class).getResult(rs, 1);
        assertThat(value).isEqualTo(7);
    }

    @Test
    void integerHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getInt(1)).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);
        Integer value = DefaultTypeHandlers.getInstance().get(Integer.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- Long ----------

    @Test
    void longHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(Long.class)
                .setParameter(ps, 1, 99L, JdbcType.BIGINT);
        verify(ps).setLong(1, 99L);
    }

    @Test
    void longHandler_getResult() throws Exception {
        when(rs.getLong(1)).thenReturn(123L);
        when(rs.wasNull()).thenReturn(false);
        Long value = DefaultTypeHandlers.getInstance().get(Long.class).getResult(rs, 1);
        assertThat(value).isEqualTo(123L);
    }

    @Test
    void longHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getLong(1)).thenReturn(0L);
        when(rs.wasNull()).thenReturn(true);
        Long value = DefaultTypeHandlers.getInstance().get(Long.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- Boolean ----------

    @Test
    void booleanHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(Boolean.class)
                .setParameter(ps, 1, true, JdbcType.BOOLEAN);
        verify(ps).setBoolean(1, true);
    }

    @Test
    void booleanHandler_getResult() throws Exception {
        when(rs.getBoolean(1)).thenReturn(true);
        when(rs.wasNull()).thenReturn(false);
        Boolean value = DefaultTypeHandlers.getInstance().get(Boolean.class).getResult(rs, 1);
        assertThat(value).isTrue();
    }

    @Test
    void booleanHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getBoolean(1)).thenReturn(false);
        when(rs.wasNull()).thenReturn(true);
        Boolean value = DefaultTypeHandlers.getInstance().get(Boolean.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- BigDecimal ----------

    @Test
    void bigDecimalHandler_setParameter() throws Exception {
        BigDecimal value = new BigDecimal("12.34");
        DefaultTypeHandlers.getInstance().get(BigDecimal.class)
                .setParameter(ps, 1, value, JdbcType.DECIMAL);
        verify(ps).setBigDecimal(1, value);
    }

    @Test
    void bigDecimalHandler_getResult() throws Exception {
        when(rs.getBigDecimal(1)).thenReturn(new BigDecimal("5.5"));
        BigDecimal value = DefaultTypeHandlers.getInstance().get(BigDecimal.class).getResult(rs, 1);
        assertThat(value).isEqualByComparingTo("5.5");
    }

    // ---------- Double ----------

    @Test
    void doubleHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(Double.class)
                .setParameter(ps, 1, 1.5, JdbcType.DOUBLE);
        verify(ps).setDouble(1, 1.5);
    }

    @Test
    void doubleHandler_getResult() throws Exception {
        when(rs.getDouble(1)).thenReturn(2.5);
        when(rs.wasNull()).thenReturn(false);
        Double value = DefaultTypeHandlers.getInstance().get(Double.class).getResult(rs, 1);
        assertThat(value).isEqualTo(2.5);
    }

    @Test
    void doubleHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getDouble(1)).thenReturn(0.0);
        when(rs.wasNull()).thenReturn(true);
        Double value = DefaultTypeHandlers.getInstance().get(Double.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- Float ----------

    @Test
    void floatHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(Float.class)
                .setParameter(ps, 1, 3.14f, JdbcType.FLOAT);
        verify(ps).setFloat(1, 3.14f);
    }

    @Test
    void floatHandler_getResult() throws Exception {
        when(rs.getFloat(1)).thenReturn(6.28f);
        when(rs.wasNull()).thenReturn(false);
        Float value = DefaultTypeHandlers.getInstance().get(Float.class).getResult(rs, 1);
        assertThat(value).isEqualTo(6.28f);
    }

    @Test
    void floatHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getFloat(1)).thenReturn(0.0f);
        when(rs.wasNull()).thenReturn(true);
        Float value = DefaultTypeHandlers.getInstance().get(Float.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- Short ----------

    @Test
    void shortHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(Short.class)
                .setParameter(ps, 1, (short) 8, JdbcType.SMALLINT);
        verify(ps).setShort(1, (short) 8);
    }

    @Test
    void shortHandler_getResult() throws Exception {
        when(rs.getShort(1)).thenReturn((short) 9);
        when(rs.wasNull()).thenReturn(false);
        Short value = DefaultTypeHandlers.getInstance().get(Short.class).getResult(rs, 1);
        assertThat(value).isEqualTo((short) 9);
    }

    @Test
    void shortHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getShort(1)).thenReturn((short) 0);
        when(rs.wasNull()).thenReturn(true);
        Short value = DefaultTypeHandlers.getInstance().get(Short.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- Byte ----------

    @Test
    void byteHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(Byte.class)
                .setParameter(ps, 1, (byte) 1, JdbcType.TINYINT);
        verify(ps).setByte(1, (byte) 1);
    }

    @Test
    void byteHandler_getResult() throws Exception {
        when(rs.getByte(1)).thenReturn((byte) 2);
        when(rs.wasNull()).thenReturn(false);
        Byte value = DefaultTypeHandlers.getInstance().get(Byte.class).getResult(rs, 1);
        assertThat(value).isEqualTo((byte) 2);
    }

    @Test
    void byteHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getByte(1)).thenReturn((byte) 0);
        when(rs.wasNull()).thenReturn(true);
        Byte value = DefaultTypeHandlers.getInstance().get(Byte.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- Character ----------

    @Test
    void characterHandler_setParameter() throws Exception {
        DefaultTypeHandlers.getInstance().get(Character.class)
                .setParameter(ps, 1, 'a', JdbcType.CHAR);
        verify(ps).setString(1, "a");
    }

    @Test
    void characterHandler_getResult() throws Exception {
        when(rs.getString(1)).thenReturn("ab");
        Character value = DefaultTypeHandlers.getInstance().get(Character.class).getResult(rs, 1);
        assertThat(value).isEqualTo('a');
    }

    @Test
    void characterHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getString(1)).thenReturn(null);
        Character value = DefaultTypeHandlers.getInstance().get(Character.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    @Test
    void characterHandler_getResult_whenEmpty() throws Exception {
        when(rs.getString(1)).thenReturn("");
        Character value = DefaultTypeHandlers.getInstance().get(Character.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- LocalDate ----------

    @Test
    void localDateHandler_setParameter() throws Exception {
        LocalDate date = LocalDate.of(2026, 1, 2);
        DefaultTypeHandlers.getInstance().get(LocalDate.class)
                .setParameter(ps, 1, date, JdbcType.DATE);
        verify(ps).setDate(1, Date.valueOf(date));
    }

    @Test
    void localDateHandler_getResult() throws Exception {
        when(rs.getDate(1)).thenReturn(Date.valueOf("2026-03-15"));
        LocalDate value = DefaultTypeHandlers.getInstance().get(LocalDate.class).getResult(rs, 1);
        assertThat(value).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void localDateHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getDate(1)).thenReturn(null);
        LocalDate value = DefaultTypeHandlers.getInstance().get(LocalDate.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- LocalDateTime ----------

    @Test
    void localDateTimeHandler_setParameter() throws Exception {
        LocalDateTime dt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        DefaultTypeHandlers.getInstance().get(LocalDateTime.class)
                .setParameter(ps, 1, dt, JdbcType.TIMESTAMP);
        verify(ps).setTimestamp(1, Timestamp.valueOf(dt));
    }

    @Test
    void localDateTimeHandler_getResult() throws Exception {
        Timestamp ts = Timestamp.valueOf("2026-03-15 10:20:30");
        when(rs.getTimestamp(1)).thenReturn(ts);
        LocalDateTime value = DefaultTypeHandlers.getInstance().get(LocalDateTime.class).getResult(rs, 1);
        assertThat(value).isEqualTo(LocalDateTime.of(2026, 3, 15, 10, 20, 30));
    }

    @Test
    void localDateTimeHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getTimestamp(1)).thenReturn(null);
        LocalDateTime value = DefaultTypeHandlers.getInstance().get(LocalDateTime.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- LocalTime ----------

    @Test
    void localTimeHandler_setParameter() throws Exception {
        LocalTime time = LocalTime.of(10, 20, 30);
        DefaultTypeHandlers.getInstance().get(LocalTime.class)
                .setParameter(ps, 1, time, JdbcType.TIME);
        verify(ps).setTime(1, Time.valueOf(time));
    }

    @Test
    void localTimeHandler_getResult() throws Exception {
        when(rs.getTime(1)).thenReturn(Time.valueOf("08:09:10"));
        LocalTime value = DefaultTypeHandlers.getInstance().get(LocalTime.class).getResult(rs, 1);
        assertThat(value).isEqualTo(LocalTime.of(8, 9, 10));
    }

    @Test
    void localTimeHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getTime(1)).thenReturn(null);
        LocalTime value = DefaultTypeHandlers.getInstance().get(LocalTime.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- java.util.Date ----------

    @Test
    void utilDateHandler_setParameter() throws Exception {
        java.util.Date date = new java.util.Date(1700000000000L);
        DefaultTypeHandlers.getInstance().get(java.util.Date.class)
                .setParameter(ps, 1, date, JdbcType.TIMESTAMP);
        verify(ps).setTimestamp(1, new Timestamp(date.getTime()));
    }

    @Test
    void utilDateHandler_getResult() throws Exception {
        Timestamp ts = new Timestamp(1700000000000L);
        when(rs.getTimestamp(1)).thenReturn(ts);
        java.util.Date value = DefaultTypeHandlers.getInstance().get(java.util.Date.class).getResult(rs, 1);
        assertThat(value).isEqualTo(new java.util.Date(1700000000000L));
    }

    @Test
    void utilDateHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getTimestamp(1)).thenReturn(null);
        java.util.Date value = DefaultTypeHandlers.getInstance().get(java.util.Date.class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- java.sql.Date ----------

    @Test
    void sqlDateHandler_setParameter() throws Exception {
        Date date = Date.valueOf("2026-01-01");
        DefaultTypeHandlers.getInstance().get(Date.class)
                .setParameter(ps, 1, date, JdbcType.DATE);
        verify(ps).setDate(1, date);
    }

    @Test
    void sqlDateHandler_getResult() throws Exception {
        when(rs.getDate(1)).thenReturn(Date.valueOf("2026-01-01"));
        Date value = DefaultTypeHandlers.getInstance().get(Date.class).getResult(rs, 1);
        assertThat(value).isEqualTo(Date.valueOf("2026-01-01"));
    }

    // ---------- java.sql.Time ----------

    @Test
    void sqlTimeHandler_setParameter() throws Exception {
        Time time = Time.valueOf("10:00:00");
        DefaultTypeHandlers.getInstance().get(Time.class)
                .setParameter(ps, 1, time, JdbcType.TIME);
        verify(ps).setTime(1, time);
    }

    @Test
    void sqlTimeHandler_getResult() throws Exception {
        when(rs.getTime(1)).thenReturn(Time.valueOf("10:00:00"));
        Time value = DefaultTypeHandlers.getInstance().get(Time.class).getResult(rs, 1);
        assertThat(value).isEqualTo(Time.valueOf("10:00:00"));
    }

    // ---------- java.sql.Timestamp ----------

    @Test
    void sqlTimestampHandler_setParameter() throws Exception {
        Timestamp ts = new Timestamp(1700000000000L);
        DefaultTypeHandlers.getInstance().get(Timestamp.class)
                .setParameter(ps, 1, ts, JdbcType.TIMESTAMP);
        verify(ps).setTimestamp(1, ts);
    }

    @Test
    void sqlTimestampHandler_getResult() throws Exception {
        when(rs.getTimestamp(1)).thenReturn(new Timestamp(1700000000000L));
        Timestamp value = DefaultTypeHandlers.getInstance().get(Timestamp.class).getResult(rs, 1);
        assertThat(value).isEqualTo(new Timestamp(1700000000000L));
    }

    // ---------- byte[] ----------

    @Test
    void byteArrayHandler_setParameter() throws Exception {
        byte[] data = {1, 2, 3};
        DefaultTypeHandlers.getInstance().get(byte[].class)
                .setParameter(ps, 1, data, JdbcType.BINARY);
        verify(ps).setBinaryStream(eq(1), any(ByteArrayInputStream.class), eq(3));
    }

    @Test
    void byteArrayHandler_getResult() throws Exception {
        byte[] data = {4, 5, 6};
        when(rs.getBytes(1)).thenReturn(data);
        byte[] value = DefaultTypeHandlers.getInstance().get(byte[].class).getResult(rs, 1);
        assertThat(value).containsExactly(4, 5, 6);
    }

    @Test
    void byteArrayHandler_getResult_whenSqlNull() throws Exception {
        when(rs.getBytes(1)).thenReturn(null);
        byte[] value = DefaultTypeHandlers.getInstance().get(byte[].class).getResult(rs, 1);
        assertThat(value).isNull();
    }

    // ---------- Calendar 辅助 ----------

    @Test
    void setParameter_withOtherJdbcTypeAndNonNullValue_usesTypeCode() throws Exception {
        // 非 null 值 + OTHER 类型仍走具体 set 方法
        DefaultTypeHandlers.getInstance().get(String.class)
                .setParameter(ps, 1, "x", JdbcType.OTHER);
        verify(ps).setString(1, "x");
    }

    @Test
    void setNull_usesProvidedJdbcTypeCode() throws Exception {
        DefaultTypeHandlers.getInstance().get(String.class)
                .setParameter(ps, 1, null, JdbcType.CHAR);
        verify(ps).setNull(1, Types.CHAR);
    }
}
