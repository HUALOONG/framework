package cn.jowen.framework.data.core.mapping;

import cn.jowen.framework.data.core.meta.GeneratedValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EntityMetadata} 测试：默认方法 {@code getColumnNames} 与 {@code getFieldName} 的派生行为，
 * 含命中列、未命中列与空属性集合三种情况。
 */
class EntityMetadataTest {

    /** 基于内存索引的最小实现，仅用于验证接口默认方法。 */
    private static final class StubMetadata implements EntityMetadata {

        private final Class<?> entityClass;
        private final String tableName;
        private final String idColumn;
        private final List<PropertyMetadata> properties = new ArrayList<>();
        private final Map<String, PropertyMetadata> byColumn = new HashMap<>();

        private StubMetadata(Class<?> entityClass, String tableName, String idColumn, PropertyMetadata... props) {
            this.entityClass = entityClass;
            this.tableName = tableName;
            this.idColumn = idColumn;
            for (PropertyMetadata prop : props) {
                properties.add(prop);
                byColumn.put(prop.getColumnName(), prop);
            }
        }

        @Override
        public Class<?> getEntityClass() {
            return entityClass;
        }

        @Override
        public String getTableName() {
            return tableName;
        }

        @Override
        public List<PropertyMetadata> getProperties() {
            return List.copyOf(properties);
        }

        @Override
        public String getIdColumn() {
            return idColumn;
        }

        @Override
        public PropertyMetadata getProperty(String column) {
            return byColumn.get(column);
        }
    }

    private static PropertyMetadata prop(String field, String column) {
        return new PropertyMetadata(field, String.class, JdbcType.VARCHAR,
                false, true, column, Optional.empty());
    }

    private final PropertyMetadata idProp = new PropertyMetadata("id", Long.class, JdbcType.BIGINT,
            true, false, "user_id", Optional.of(GeneratedValue.Strategy.SNOWFLAKE));

    private final EntityMetadata metadata = new StubMetadata(
            Object.class, "sys_user", "user_id",
            idProp,
            prop("userName", "user_name"),
            prop("email", "email")
    );

    @Test
    void getColumnNames_followsPropertyOrder() {
        assertThat(metadata.getColumnNames()).containsExactly("user_id", "user_name", "email");
    }

    @Test
    void getColumnNames_emptyWhenNoProperties() {
        EntityMetadata empty = new StubMetadata(Object.class, "no_props", null);

        assertThat(empty.getColumnNames()).isEmpty();
        assertThat(empty.getProperties()).isEmpty();
    }

    @Test
    void getFieldName_mapsColumnBackToField() {
        assertThat(metadata.getFieldName("user_name")).isEqualTo("userName");
        assertThat(metadata.getFieldName("user_id")).isEqualTo("id");
    }

    @Test
    void getFieldName_unknownColumn_returnsNull() {
        assertThat(metadata.getFieldName("not_exist")).isNull();
    }

    @Test
    void abstractAccessors_exposeDeclaredValues() {
        assertThat(metadata.getEntityClass()).isEqualTo(Object.class);
        assertThat(metadata.getTableName()).isEqualTo("sys_user");
        assertThat(metadata.getIdColumn()).isEqualTo("user_id");
        assertThat(metadata.getProperty("email")).isSameAs(metadata.getProperties().get(2));
    }
}
