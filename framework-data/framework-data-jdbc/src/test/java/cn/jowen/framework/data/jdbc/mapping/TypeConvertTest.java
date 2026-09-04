package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BeanPropertyRowMapper.TypeConvert} 类型转换测试。
 */
class TypeConvertTest {

    enum Status { ACTIVE, INACTIVE }

    // ===================== 基础 =====================

    @Test
    void nullValue_returnsNull() {
        assertThat(BeanPropertyRowMapper.convert(null, String.class)).isNull();
    }

    @Test
    void matchingType_returnsAsIs() {
        assertThat(BeanPropertyRowMapper.convert(42, Integer.class)).isEqualTo(42);
    }

    // ===================== Number -> =====================

    @Test
    void number_toPrimitives() {
        assertThat(BeanPropertyRowMapper.convert(1, int.class)).isEqualTo(1);
        assertThat(BeanPropertyRowMapper.convert(2L, Long.class)).isEqualTo(2L);
        assertThat(BeanPropertyRowMapper.convert(3, double.class)).isEqualTo(3.0d);
        assertThat(BeanPropertyRowMapper.convert(4, float.class)).isEqualTo(4.0f);
        assertThat(BeanPropertyRowMapper.convert(5, short.class)).isEqualTo((short) 5);
        assertThat(BeanPropertyRowMapper.convert(6, byte.class)).isEqualTo((byte) 6);
        assertThat((Boolean) BeanPropertyRowMapper.convert(7, boolean.class)).isTrue();
        assertThat((Boolean) BeanPropertyRowMapper.convert(0, Boolean.class)).isFalse();
        assertThat(BeanPropertyRowMapper.convert(8, String.class)).isEqualTo("8");
        assertThat(BeanPropertyRowMapper.convert(9, BigDecimal.class)).isEqualTo(BigDecimal.valueOf(9.0));
    }

    @Test
    void number_unsupportedTarget_throws() {
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert(1, LocalDate.class))
                .isInstanceOf(DataAccessException.class);
    }

    // ===================== String -> =====================

    @Test
    void string_toPrimitives() {
        assertThat(BeanPropertyRowMapper.convert("42", int.class)).isEqualTo(42);
        assertThat(BeanPropertyRowMapper.convert("42", Integer.class)).isEqualTo(42);
        assertThat(BeanPropertyRowMapper.convert("42", long.class)).isEqualTo(42L);
        assertThat(BeanPropertyRowMapper.convert("4.5", double.class)).isEqualTo(4.5d);
        assertThat(BeanPropertyRowMapper.convert("4.5", float.class)).isEqualTo(4.5f);
        assertThat(BeanPropertyRowMapper.convert("7", short.class)).isEqualTo((short) 7);
        assertThat(BeanPropertyRowMapper.convert("8", byte.class)).isEqualTo((byte) 8);
        assertThat((Boolean) BeanPropertyRowMapper.convert("true", boolean.class)).isTrue();
        assertThat((Boolean) BeanPropertyRowMapper.convert("true", Boolean.class)).isTrue();
        assertThat(BeanPropertyRowMapper.convert("9", BigDecimal.class)).isEqualTo(new BigDecimal("9"));
    }

    @Test
    void string_toChar() {
        assertThat(BeanPropertyRowMapper.convert("a", char.class)).isEqualTo('a');
        assertThat(BeanPropertyRowMapper.convert("a", Character.class)).isEqualTo('a');
        assertThat(BeanPropertyRowMapper.convert("", char.class)).isEqualTo(' ');
    }

    @Test
    void string_toTimeTypes() {
        assertThat(BeanPropertyRowMapper.convert("2026-01-02", LocalDate.class))
                .isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(BeanPropertyRowMapper.convert("2026-01-02T03:04:05", LocalDateTime.class))
                .isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        assertThat(BeanPropertyRowMapper.convert("03:04:05", LocalTime.class))
                .isEqualTo(LocalTime.of(3, 4, 5));
        assertThat(BeanPropertyRowMapper.convert("2026-01-02", java.sql.Date.class))
                .isEqualTo(java.sql.Date.valueOf("2026-01-02"));
        assertThat(BeanPropertyRowMapper.convert("2026-01-02T03:04:05", Timestamp.class))
                .isEqualTo(Timestamp.valueOf("2026-01-02 03:04:05"));
        assertThat(BeanPropertyRowMapper.convert("2026-01-02T03:04:05", Date.class))
                .isInstanceOf(Date.class);
    }

    @Test
    void string_toEnum() {
        assertThat(BeanPropertyRowMapper.convert("ACTIVE", Status.class)).isEqualTo(Status.ACTIVE);
    }

    @Test
    void string_unparsable_throwsWrapped() {
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert("abc", int.class))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert("abc", LocalDate.class))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void string_unsupportedTarget_throws() {
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert("x", StringBuilder.class))
                .isInstanceOf(DataAccessException.class);
    }

    // ===================== Timestamp -> =====================

    @Test
    void timestamp_toAll() {
        Timestamp ts = Timestamp.valueOf("2026-01-02 03:04:05");
        assertThat(BeanPropertyRowMapper.convert(ts, LocalDateTime.class)).isEqualTo(ts.toLocalDateTime());
        assertThat(BeanPropertyRowMapper.convert(ts, LocalDate.class)).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(BeanPropertyRowMapper.convert(ts, LocalTime.class)).isEqualTo(LocalTime.of(3, 4, 5));
        assertThat(BeanPropertyRowMapper.convert(ts, java.sql.Date.class)).isEqualTo(java.sql.Date.valueOf("2026-01-02"));
        assertThat(BeanPropertyRowMapper.convert(ts, java.sql.Time.class)).isEqualTo(java.sql.Time.valueOf("03:04:05"));
        assertThat(BeanPropertyRowMapper.convert(ts, Timestamp.class)).isSameAs(ts);
        assertThat(BeanPropertyRowMapper.convert(ts, Date.class)).isInstanceOf(Date.class);
        assertThat(BeanPropertyRowMapper.convert(ts, String.class)).isEqualTo(ts.toString());
    }

    @Test
    void timestamp_unsupportedTarget_throws() {
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert(Timestamp.valueOf("2026-01-02 03:04:05"), Integer.class))
                .isInstanceOf(DataAccessException.class);
    }

    // ===================== java.sql.Date -> =====================

    @Test
    void sqlDate_toAll() {
        java.sql.Date d = java.sql.Date.valueOf("2026-01-02");
        assertThat(BeanPropertyRowMapper.convert(d, LocalDate.class)).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(BeanPropertyRowMapper.convert(d, LocalDateTime.class)).isEqualTo(LocalDateTime.of(2026, 1, 2, 0, 0));
        assertThat(BeanPropertyRowMapper.convert(d, java.sql.Date.class)).isSameAs(d);
        assertThat(BeanPropertyRowMapper.convert(d, Date.class)).isInstanceOf(Date.class);
        assertThat(BeanPropertyRowMapper.convert(d, String.class)).isEqualTo(d.toString());
    }

    @Test
    void sqlDate_unsupportedTarget_throws() {
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert(java.sql.Date.valueOf("2026-01-02"), Integer.class))
                .isInstanceOf(DataAccessException.class);
    }

    // ===================== java.sql.Time -> =====================

    @Test
    void sqlTime_toAll() {
        java.sql.Time t = java.sql.Time.valueOf("03:04:05");
        assertThat(BeanPropertyRowMapper.convert(t, LocalTime.class)).isEqualTo(LocalTime.of(3, 4, 5));
        assertThat(BeanPropertyRowMapper.convert(t, java.sql.Time.class)).isSameAs(t);
        assertThat(BeanPropertyRowMapper.convert(t, Date.class)).isInstanceOf(Date.class);
        assertThat(BeanPropertyRowMapper.convert(t, String.class)).isEqualTo(t.toString());
    }

    @Test
    void sqlTime_unsupportedTarget_throws() {
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert(java.sql.Time.valueOf("03:04:05"), Integer.class))
                .isInstanceOf(DataAccessException.class);
    }

    // ===================== LocalDate / LocalDateTime / LocalTime -> =====================

    @Test
    void localDate_toDerived() {
        LocalDate ld = LocalDate.of(2026, 1, 2);
        assertThat(BeanPropertyRowMapper.convert(ld, LocalDateTime.class)).isEqualTo(ld.atStartOfDay());
        assertThat(BeanPropertyRowMapper.convert(ld, java.sql.Date.class)).isEqualTo(java.sql.Date.valueOf("2026-01-02"));
        // 非派生类型且不匹配 -> 抛异常
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert(ld, LocalTime.class))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void localDateTime_toDerived() {
        LocalDateTime ldt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        assertThat(BeanPropertyRowMapper.convert(ldt, LocalDate.class)).isEqualTo(ldt.toLocalDate());
        assertThat(BeanPropertyRowMapper.convert(ldt, LocalTime.class)).isEqualTo(ldt.toLocalTime());
        assertThat(BeanPropertyRowMapper.convert(ldt, Timestamp.class)).isEqualTo(Timestamp.valueOf("2026-01-02 03:04:05"));
        assertThat(BeanPropertyRowMapper.convert(ldt, Date.class)).isInstanceOf(Date.class);
        // 未匹配分支
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert(ldt, StringBuilder.class))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void localTime_toSqlTime() {
        LocalTime lt = LocalTime.of(3, 4, 5);
        assertThat(BeanPropertyRowMapper.convert(lt, java.sql.Time.class)).isEqualTo(java.sql.Time.valueOf("03:04:05"));
    }

    // ===================== 私有 fromXxx 同类型返回分支（反射覆盖） =====================
    // convert() 对「已是目标类型」的值会在入口处直接返回（line 160），因此 fromXxx 的
    // 「targetType 与入参同类型」返回分支只能通过直接调用私有方法覆盖。

    @Test
    void fromString_sameTypeTarget_returnsString() throws Exception {
        assertThat(invoke("fromString", "x", String.class, String.class)).isEqualTo("x");
    }

    @Test
    void fromTimestamp_sameTypeTargets() throws Exception {
        java.sql.Timestamp ts = java.sql.Timestamp.valueOf("2026-01-02 03:04:05");
        assertThat(invoke("fromTimestamp", ts, java.sql.Timestamp.class, java.sql.Timestamp.class)).isSameAs(ts);
        Object asDate = invoke("fromTimestamp", ts, java.sql.Timestamp.class, Date.class);
        assertThat(asDate).isInstanceOf(Date.class);
    }

    @Test
    void fromSqlDate_sameTypeTargets() throws Exception {
        java.sql.Date d = java.sql.Date.valueOf("2026-01-02");
        assertThat(invoke("fromSqlDate", d, java.sql.Date.class, java.sql.Date.class)).isSameAs(d);
        Object asDate = invoke("fromSqlDate", d, java.sql.Date.class, Date.class);
        assertThat(asDate).isInstanceOf(Date.class);
    }

    @Test
    void fromSqlTime_sameTypeTargets() throws Exception {
        java.sql.Time t = java.sql.Time.valueOf("03:04:05");
        assertThat(invoke("fromSqlTime", t, java.sql.Time.class, java.sql.Time.class)).isSameAs(t);
        Object asDate = invoke("fromSqlTime", t, java.sql.Time.class, Date.class);
        assertThat(asDate).isInstanceOf(Date.class);
    }

    // ===================== 不可转换（最终抛异常分支） =====================

    @Test
    void convert_trulyInconvertible_throws() {
        // 既不是 Number/String/时间类型，也不匹配任何派生分支 -> 最终抛异常
        assertThatThrownBy(() -> BeanPropertyRowMapper.convert(new Object(), Integer.class))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无法转换类型");
    }

    @SuppressWarnings("unchecked")
    private static Object invoke(String method, Object arg, Class<?> argType, Class<?> targetType) throws Exception {
        java.lang.reflect.Method m = BeanPropertyRowMapper.TypeConvert.class
                .getDeclaredMethod(method, argType, Class.class);
        m.setAccessible(true);
        return m.invoke(null, arg, targetType);
    }
}
