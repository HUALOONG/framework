package cn.jowen.framework.data.core.mapping;

import org.jspecify.annotations.NullMarked;

/**
 * 实体元信息解析器，将实体类解析为 {@link EntityMetadata}。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public interface EntityMetadataResolver {

    /**
     * 解析实体类元信息（实现应缓存结果）。
     *
     * @param entityClass 实体类，不可为 {@code null}
     * @return 元信息，不可为 {@code null}
     */
    EntityMetadata resolve(Class<?> entityClass);
}
