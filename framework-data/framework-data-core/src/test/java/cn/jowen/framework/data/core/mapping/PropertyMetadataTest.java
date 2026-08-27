package cn.jowen.framework.data.core.mapping;

import cn.jowen.framework.data.core.meta.GeneratedValue;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class PropertyMetadataTest {

    @Test
    void idProperty_withGenerator() {
        PropertyMetadata prop = new PropertyMetadata(
            "id", Long.class, JdbcType.BIGINT,
            true, false, "user_id",
            Optional.of(GeneratedValue.Strategy.SNOWFLAKE)
        );

        assertThat(prop.getName()).isEqualTo("id");
        assertThat(prop.getJavaType()).isEqualTo(Long.class);
        assertThat(prop.getJdbcType()).isEqualTo(JdbcType.BIGINT);
        assertThat(prop.isId()).isTrue();
        assertThat(prop.isNullable()).isFalse();
        assertThat(prop.getColumnName()).isEqualTo("user_id");
        assertThat(prop.getGenerated()).contains(GeneratedValue.Strategy.SNOWFLAKE);
        assertThat(prop.isGenerated()).isTrue();
    }

    @Test
    void nonIdProperty() {
        PropertyMetadata prop = new PropertyMetadata(
            "name", String.class, JdbcType.VARCHAR,
            false, true, "user_name",
            Optional.empty()
        );

        assertThat(prop.getName()).isEqualTo("name");
        assertThat(prop.getJavaType()).isEqualTo(String.class);
        assertThat(prop.getJdbcType()).isEqualTo(JdbcType.VARCHAR);
        assertThat(prop.isId()).isFalse();
        assertThat(prop.isNullable()).isTrue();
        assertThat(prop.getColumnName()).isEqualTo("user_name");
        assertThat(prop.getGenerated()).isEmpty();
        assertThat(prop.isGenerated()).isFalse();
    }

    @Test
    void idWithoutGenerator_notGenerated() {
        PropertyMetadata prop = new PropertyMetadata(
            "id", Long.class, null,
            true, false, "id",
            Optional.empty()
        );
        assertThat(prop.isGenerated()).isFalse();
    }

    @Test
    void nullableJdbcType() {
        PropertyMetadata prop = new PropertyMetadata(
            "field", String.class, null,
            false, true, "field",
            Optional.empty()
        );
        assertThat(prop.getJdbcType()).isNull();
    }
}
