package cn.jowen.framework.data.core.repository;

import org.jspecify.annotations.NullMarked;

/**
 * 仓储工厂，按实体类型创建 {@link Repository} 实例。各实现模块提供对应工厂实现（经 SPI 或装配层注册）。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public interface RepositoryFactory {

    /**
     * 创建指定实体类型的仓储。
     *
     * @param entityClass 实体类，不可为 {@code null}
     * @param <T>         实体类型
     * @param <ID>        主键类型
     * @return 仓储实例，不可为 {@code null}
     */
    <T, ID> Repository<T, ID> getRepository(Class<T> entityClass);
}
