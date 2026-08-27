package cn.jowen.framework.data.core.meta;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

class AnnotationRetentionTest {

    static class SampleEntity {
        @Id
        @GeneratedValue(GeneratedValue.Strategy.SNOWFLAKE)
        @Column("user_id")
        private Long id;

        @Column(value = "user_name", ignore = false)
        private String name;

        @Column(ignore = true)
        private String ignored;
    }

    @Table("t_user")
    static class AnnotatedTable {
    }

    @Test
    void tableAnnotation_readable() {
        Table table = AnnotatedTable.class.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(table.value()).isEqualTo("t_user");
    }

    @Test
    void idAnnotation_readable() throws NoSuchFieldException {
        Field id = SampleEntity.class.getDeclaredField("id");
        assertThat(id.getAnnotation(Id.class)).isNotNull();
    }

    @Test
    void generatedValueAnnotation_readable() throws NoSuchFieldException {
        Field id = SampleEntity.class.getDeclaredField("id");
        GeneratedValue gen = id.getAnnotation(GeneratedValue.class);
        assertThat(gen).isNotNull();
        assertThat(gen.value()).isEqualTo(GeneratedValue.Strategy.SNOWFLAKE);
    }

    @Test
    void generatedValue_defaultStrategy() {
        assertThat(GeneratedValue.Strategy.AUTO).isEqualTo(GeneratedValue.Strategy.AUTO);
    }

    @Test
    void columnAnnotation_readable() throws NoSuchFieldException {
        Field name = SampleEntity.class.getDeclaredField("name");
        Column col = name.getAnnotation(Column.class);
        assertThat(col).isNotNull();
        assertThat(col.value()).isEqualTo("user_name");
        assertThat(col.ignore()).isFalse();
    }

    @Test
    void columnIgnore_readable() throws NoSuchFieldException {
        Field ignored = SampleEntity.class.getDeclaredField("ignored");
        Column col = ignored.getAnnotation(Column.class);
        assertThat(col).isNotNull();
        assertThat(col.ignore()).isTrue();
        assertThat(col.value()).isEmpty();
    }
}
