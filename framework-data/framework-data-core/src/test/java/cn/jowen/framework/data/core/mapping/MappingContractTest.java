package cn.jowen.framework.data.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 接口契约测试：用轻量 fake 验证 {@link EntityMetadata}、{@link EntityMetadataResolver} 与
 * {@link RowMapper} 的形态自洽。
 */
class MappingContractTest {

    record Person(String id, String name) {}

    static final class FakeEntityMetadata implements EntityMetadata {
        private final Class<?> entityClass;

        FakeEntityMetadata(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public Class<?> getEntityClass() {
            return entityClass;
        }

        @Override
        public String getTableName() {
            return "t_" + entityClass.getSimpleName().toLowerCase();
        }

        @Override
        public List<String> getColumnNames() {
            return List.of("id", "name");
        }

        @Override
        public String getIdColumn() {
            return "id";
        }

        @Override
        public String getFieldName(String column) {
            return switch (column) {
                case "id" -> "id";
                case "name" -> "name";
                default -> null;
            };
        }
    }

    static final class FakeEntityMetadataResolver implements EntityMetadataResolver {
        @Override
        public EntityMetadata resolve(Class<?> entityClass) {
            return new FakeEntityMetadata(entityClass);
        }
    }

    static final class MapRowMapper implements RowMapper<Person> {
        @Override
        public Person mapRow(Map<String, Object> row, int rowNum) {
            return new Person((String) row.get("id"), (String) row.get("name"));
        }
    }

    @Test
    void resolverReturnsMetadataForClass() {
        EntityMetadataResolver resolver = new FakeEntityMetadataResolver();
        EntityMetadata meta = resolver.resolve(Person.class);
        assertThat(meta.getEntityClass()).isEqualTo(Person.class);
        assertThat(meta.getTableName()).isEqualTo("t_person");
        assertThat(meta.getColumnNames()).containsExactly("id", "name");
        assertThat(meta.getIdColumn()).isEqualTo("id");
        assertThat(meta.getFieldName("name")).isEqualTo("name");
        assertThat(meta.getFieldName("unknown")).isNull();
    }

    @Test
    void rowMapperMapsMapToObject() {
        MapRowMapper mapper = new MapRowMapper();
        Person p = mapper.mapRow(Map.of("id", "1", "name", "Alice"), 0);
        assertThat(p).isEqualTo(new Person("1", "Alice"));
    }

    @Test
    void rowMapperReceivesRowNumber() {
        MapRowMapper mapper = new MapRowMapper();
        Person p = mapper.mapRow(Map.of("id", "2", "name", "Bob"), 7);
        assertThat(p).isEqualTo(new Person("2", "Bob"));
    }
}
