package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.mapping.NamingStrategy;
import org.jspecify.annotations.NullMarked;

/**
 * 实体元信息解析便捷入口。
 *
 * <p>委托给 {@link DefaultEntityMetadataResolver}，提供静态工具方法，便于在仓库层与映射层统一获取实体元信息，
 * 避免各处重复创建解析器实例。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class EntityMetadataParser {

    private static final DefaultEntityMetadataResolver DEFAULT =
            new DefaultEntityMetadataResolver(new CamelCaseNamingStrategy());

    private EntityMetadataParser() {
    }

    /**
     * 使用默认命名策略解析实体类。
     *
     * @param entityClass 实体类，不可为 {@code null}
     * @return 元信息，不可为 {@code null}
     */
    public static EntityMetadata parse(Class<?> entityClass) {
        return DEFAULT.resolve(entityClass);
    }

    /**
     * 使用指定命名策略解析实体类。
     *
     * @param entityClass    实体类，不可为 {@code null}
     * @param namingStrategy 命名策略，不可为 {@code null}
     * @return 元信息，不可为 {@code null}
     */
    public static EntityMetadata parse(Class<?> entityClass, NamingStrategy namingStrategy) {
        return new DefaultEntityMetadataResolver(namingStrategy).resolve(entityClass);
    }

    /**
     * 使用指定解析器解析实体类。
     *
     * @param entityClass 实体类，不可为 {@code null}
     * @param resolver    解析器，不可为 {@code null}
     * @return 元信息，不可为 {@code null}
     */
    public static EntityMetadata parse(Class<?> entityClass, EntityMetadataResolver resolver) {
        return resolver.resolve(entityClass);
    }
}
