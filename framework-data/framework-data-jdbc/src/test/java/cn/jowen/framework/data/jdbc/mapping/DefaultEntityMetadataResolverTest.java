package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultEntityMetadataResolver} 单元测试。
 */
class DefaultEntityMetadataResolverTest {

    @Table("resolved_user")
    static class ResolvedUser {
        @Id
        @GeneratedValue(GeneratedValue.Strategy.AUTO)
        private Long id;
        @Column("display_name")
        private String displayName;
    }

    static class PlainEntity {
        private String simpleField;
    }

    private final DefaultEntityMetadataResolver resolver = new DefaultEntityMetadataResolver();

    @Test
    void resolve_parsesEntity() {
        EntityMetadata metadata = resolver.resolve(ResolvedUser.class);
        assertThat(metadata.getTableName()).isEqualTo("resolved_user");
        assertThat(metadata.getIdColumn()).isEqualTo("id");
        assertThat(metadata.getProperties()).extracting(m -> m.getName())
                .containsExactly("id", "displayName");
    }

    @Test
    void resolve_cached_returnsSameInstance() {
        EntityMetadata first = resolver.resolve(PlainEntity.class);
        EntityMetadata second = resolver.resolve(PlainEntity.class);
        assertThat(second).isSameAs(first);
    }

    @Test
    void clearCache_reResolves() {
        EntityMetadata first = resolver.resolve(PlainEntity.class);
        resolver.clearCache();
        EntityMetadata second = resolver.resolve(PlainEntity.class);
        assertThat(second).isNotSameAs(first);
        assertThat(second.getTableName()).isEqualTo(first.getTableName());
    }

    @Test
    void resolve_plainEntity_usesNamingStrategy() {
        EntityMetadata metadata = resolver.resolve(PlainEntity.class);
        // CamelCaseNamingStrategy 会去除 Entity 后缀：PlainEntity -> plain
        assertThat(metadata.getTableName()).isEqualTo("plain");
        assertThat(metadata.getIdColumn()).isNull();
    }
}
