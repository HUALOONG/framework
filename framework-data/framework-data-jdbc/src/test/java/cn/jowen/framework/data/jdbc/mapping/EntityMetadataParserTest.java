package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.mapping.NamingStrategy;
import cn.jowen.framework.data.core.mapping.PropertyMetadata;
import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EntityMetadataParser} 单元测试。
 */
class EntityMetadataParserTest {

    @Table("t_user")
    static class User {
        @Id
        @GeneratedValue(GeneratedValue.Strategy.AUTO)
        private Long id;
        private String userName;
        @Column("nick_name")
        private String nickName;
        @Column(ignore = true)
        private String ignoredField;
        private static String staticField;
        private transient String transientField;
    }

    static class OrderItem {
        private String itemName;
    }

    @Test
    void parse_defaultStrategy_usesExplicitTableName() {
        EntityMetadata metadata = EntityMetadataParser.parse(User.class);
        assertThat(metadata.getTableName()).isEqualTo("t_user");
        assertThat(metadata.getEntityClass()).isEqualTo(User.class);
        assertThat(metadata.getIdColumn()).isEqualTo("id");
    }

    @Test
    void parse_defaultStrategy_mapsProperties() {
        EntityMetadata metadata = EntityMetadataParser.parse(User.class);
        List<PropertyMetadata> properties = metadata.getProperties();
        assertThat(properties).extracting(PropertyMetadata::getName)
                .containsExactly("id", "userName", "nickName");
    }

    @Test
    void parse_defaultStrategy_mapsColumns() {
        EntityMetadata metadata = EntityMetadataParser.parse(User.class);
        assertThat(metadata.getColumnNames()).containsExactly("id", "user_name", "nick_name");
        PropertyMetadata nickName = metadata.getProperty("nick_name");
        assertThat(nickName).isNotNull();
        assertThat(nickName.getName()).isEqualTo("nickName");
    }

    @Test
    void parse_defaultStrategy_excludesIgnoredStaticTransient() {
        EntityMetadata metadata = EntityMetadataParser.parse(User.class);
        List<PropertyMetadata> properties = metadata.getProperties();
        assertThat(properties)
                .extracting(PropertyMetadata::getName)
                .doesNotContain("ignoredField", "staticField", "transientField");
    }

    @Test
    void parse_defaultStrategy_infersTableNameFromClassName() {
        EntityMetadata metadata = EntityMetadataParser.parse(OrderItem.class);
        assertThat(metadata.getTableName()).isEqualTo("order_item");
        assertThat(metadata.getColumnNames()).containsExactly("item_name");
        assertThat(metadata.getIdColumn()).isNull();
    }

    @Test
    void parse_defaultStrategy_idGenerated() {
        EntityMetadata metadata = EntityMetadataParser.parse(User.class);
        PropertyMetadata id = metadata.getProperty("id");
        assertThat(id).isNotNull();
        assertThat(id.isId()).isTrue();
        assertThat(id.isGenerated()).isTrue();
        assertThat(id.getGenerated()).contains(GeneratedValue.Strategy.AUTO);
        assertThat(id.isNullable()).isFalse();
    }

    @Test
    void parse_withNamingStrategy() {
        NamingStrategy naming = mock(NamingStrategy.class);
        when(naming.toTableName("OrderItem")).thenReturn("t_order_item");
        when(naming.toColumnName("itemName")).thenReturn("ITEM_NAME");

        EntityMetadata metadata = EntityMetadataParser.parse(OrderItem.class, naming);

        assertThat(metadata.getTableName()).isEqualTo("t_order_item");
        assertThat(metadata.getColumnNames()).containsExactly("ITEM_NAME");
        verify(naming).toTableName("OrderItem");
        verify(naming).toColumnName("itemName");
    }

    @Test
    void parse_withCustomResolver() {
        EntityMetadataResolver resolver = mock(EntityMetadataResolver.class);
        EntityMetadata expected = mock(EntityMetadata.class);
        when(resolver.resolve(User.class)).thenReturn(expected);

        EntityMetadata actual = EntityMetadataParser.parse(User.class, resolver);

        assertThat(actual).isSameAs(expected);
        verify(resolver).resolve(User.class);
    }

    @Test
    void defaultResolver_usesCamelCaseTableForPlainClass() {
        EntityMetadata metadata = EntityMetadataParser.parse(User.class);
        assertThat(metadata.getTableName()).isEqualTo("t_user");
        assertThat(metadata.getFieldName("user_name")).isEqualTo("userName");
        assertThat(metadata.getFieldName("not_exists")).isNull();
    }
}
