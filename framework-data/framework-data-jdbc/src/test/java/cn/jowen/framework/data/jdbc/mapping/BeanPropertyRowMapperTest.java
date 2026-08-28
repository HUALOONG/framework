package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.mapping.PropertyMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link BeanPropertyRowMapper} 单元测试。
 */
class BeanPropertyRowMapperTest {

    // ===================== 测试实体 =====================

    static class User {
        private Long id;
        private String userName;
        private Integer age;
        private Boolean active;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    record UserRecord(Long id, String userName) {
    }

    static class SingleArgCtor {
        private final String arg0;
        SingleArgCtor(String arg0) {
            this.arg0 = arg0;
        }
        String getValue() { return arg0; }
    }

    static class ExplodingCtor {
        ExplodingCtor() {
            throw new IllegalStateException("ctor boom");
        }
    }

    interface Marker {
    }

    // ===================== mapRow 基础 =====================

    @Test
    void mapRow_nullRow_returnsNull() {
        assertThat(BeanPropertyRowMapper.of(User.class).mapRow(null, 0)).isNull();
    }

    @Test
    void mapRow_mapsSnakeCaseColumnsToBean() {
        Map<String, Object> row = Map.of(
                "id", 1L,
                "user_name", "jowen",
                "age", 30,
                "active", true
        );
        User user = BeanPropertyRowMapper.of(User.class).mapRow(row, 0);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUserName()).isEqualTo("jowen");
        assertThat(user.getAge()).isEqualTo(30);
        assertThat(user.getActive()).isTrue();
    }

    @Test
    void mapRow_skipsNullValues() {
        Map<String, Object> row = Map.of("id", 1L);
        User user = BeanPropertyRowMapper.of(User.class).mapRow(row, 0);
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUserName()).isNull();
        assertThat(user.getAge()).isNull();
    }

    @Test
    void mapRow_convertsStringNumberToField() {
        // 字符串数值自动转换为 Long
        Map<String, Object> row = Map.of("id", "42");
        User user = BeanPropertyRowMapper.of(User.class).mapRow(row, 0);
        assertThat(user.getId()).isEqualTo(42L);
    }

    @Test
    void mapRow_emptyRow_returnsEmptyBean() {
        User user = BeanPropertyRowMapper.of(User.class).mapRow(Map.of(), 0);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNull();
    }

    // ===================== 构造器路径 =====================

    @Test
    void mapRow_usesCustomResolver() {
        EntityMetadataResolver resolver = mock(EntityMetadataResolver.class);
        when(resolver.resolve(User.class)).thenReturn(metaForUser());
        User user = BeanPropertyRowMapper.of(User.class, resolver).mapRow(
                Map.of("id", 7L, "user_name", "custom"), 0);
        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getUserName()).isEqualTo("custom");
    }

    @Test
    void mapRow_record_fallsBackToConstructor() {
        // record 无无参构造，走 instantiateByConstructor；
        // record 构造器参数名保留组件名 id/userName；
        // 但 record 字段为 final，实例化后字段写入会失败并被包装
        EntityMetadataResolver resolver = mock(EntityMetadataResolver.class);
        when(resolver.resolve(UserRecord.class)).thenReturn(new DefaultEntityMetadata(
                UserRecord.class, "user_record",
                List.of(
                        new PropertyMetadata("id", Long.class, null, false, true, "id", Optional.empty()),
                        new PropertyMetadata("userName", String.class, null, false, true, "user_name", Optional.empty())),
                null));
        assertThatThrownBy(() -> BeanPropertyRowMapper.of(UserRecord.class, resolver).mapRow(
                Map.of("id", 9L, "user_name", "rec"), 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("映射字段失败");
    }

    @Test
    void mapRow_singleArgCtor_fallsBackToConstructor() {
        // 普通类未开 -parameters 时构造器参数名为 arg0，实体字段名亦为 arg0
        EntityMetadataResolver resolver = mock(EntityMetadataResolver.class);
        when(resolver.resolve(SingleArgCtor.class)).thenReturn(new DefaultEntityMetadata(
                SingleArgCtor.class, "single_arg",
                List.of(new PropertyMetadata("arg0", String.class, null, false, true, "value", Optional.empty())),
                null));
        SingleArgCtor ctor = BeanPropertyRowMapper.of(SingleArgCtor.class, resolver).mapRow(
                Map.of("value", "hello"), 0);
        assertThat(ctor.getValue()).isEqualTo("hello");
    }

    @Test
    void mapRow_interfaceType_throwsNoConstructor() {
        // 接口无构造器：无参构造与构造器回退均失败
        EntityMetadataResolver resolver = mock(EntityMetadataResolver.class);
        when(resolver.resolve(Marker.class)).thenReturn(new DefaultEntityMetadata(
                Marker.class, "marker", List.of(), null));
        assertThatThrownBy(() -> BeanPropertyRowMapper.of(Marker.class, resolver)
                .mapRow(Map.of(), 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无可用构造器");
    }

    @Test
    void mapRow_constructorParamWithoutProperty_throws() {
        EntityMetadataResolver resolver = mock(EntityMetadataResolver.class);
        when(resolver.resolve(SingleArgCtor.class)).thenReturn(new DefaultEntityMetadata(
                SingleArgCtor.class, "single_arg",
                List.of(new PropertyMetadata("other", String.class, null, false, true, "value", Optional.empty())),
                null));
        assertThatThrownBy(() -> BeanPropertyRowMapper.of(SingleArgCtor.class, resolver)
                .mapRow(Map.of("value", "hello"), 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无对应实体属性");
    }

    @Test
    void mapRow_constructorThrows_wrapsDataAccessException() {
        // 无无参构造回退后构造器抛异常
        EntityMetadataResolver resolver = mock(EntityMetadataResolver.class);
        when(resolver.resolve(ExplodingCtor.class)).thenReturn(new DefaultEntityMetadata(
                ExplodingCtor.class, "exploding",
                List.of(), null));
        assertThatThrownBy(() -> BeanPropertyRowMapper.of(ExplodingCtor.class, resolver).mapRow(Map.of(), 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("实例化实体失败");
    }

    // ===================== 写入失败 =====================

    @Test
    void mapRow_missingFieldOnBean_throwsDataAccessException() {
        // 元信息声明了实体上不存在的字段 -> setFieldValue 失败被包装
        EntityMetadataResolver resolver = mock(EntityMetadataResolver.class);
        when(resolver.resolve(User.class)).thenReturn(new DefaultEntityMetadata(
                User.class, "user",
                List.of(new PropertyMetadata("ghost", String.class, null, false, true, "ghost", Optional.empty())),
                null));
        assertThatThrownBy(() -> BeanPropertyRowMapper.of(User.class, resolver)
                .mapRow(Map.of("ghost", "x"), 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("映射字段失败");
    }

    // ===================== 类型转换补充 =====================

    @Test
    void convert_unsupportedObjectToString() {
        // Boolean 非 Number/String/时间类型 -> 走 targetType == String 分支
        assertThat(BeanPropertyRowMapper.convert(Boolean.TRUE, String.class)).isEqualTo("true");
        assertThat(BeanPropertyRowMapper.convert(Boolean.FALSE, String.class)).isEqualTo("false");
    }

    @Test
    void convert_enumFromString_uppercaseTrim() {
        assertThat(BeanPropertyRowMapper.convert("  ACTIVE  ", TypeConvertTest.Status.class))
                .isEqualTo(TypeConvertTest.Status.ACTIVE);
    }

    @Test
    void convert_numericToWiderTypes() {
        // Integer -> Long 走 fromNumber 的 longValue 分支
        assertThat(BeanPropertyRowMapper.convert(42, Long.class)).isEqualTo(42L);
        // Integer -> Double 走 fromNumber 的 doubleValue 分支
        assertThat(BeanPropertyRowMapper.convert(3, Double.class)).isEqualTo(3.0d);
    }

    private EntityMetadata metaForUser() {
        return new DefaultEntityMetadata(
                User.class, "user",
                List.of(
                        new PropertyMetadata("id", Long.class, null, true, false, "id", Optional.empty()),
                        new PropertyMetadata("userName", String.class, null, false, true, "user_name", Optional.empty())),
                "id");
    }
}
