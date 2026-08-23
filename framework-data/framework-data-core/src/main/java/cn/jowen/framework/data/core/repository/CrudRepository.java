package cn.jowen.framework.data.core.repository;

import org.jspecify.annotations.NullMarked;

/**
 * CRUD 仓储接口，继承 {@link Repository}，补充更新与批量删除能力。
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface CrudRepository<T, ID> extends Repository<T, ID> {

    /**
     * 更新已存在的实体（按主键）。
     *
     * @param entity 实体，不可为 {@code null}
     * @return 更新后的实体
     */
    T update(T entity);

    /**
     * 删除指定实体。
     *
     * @param entity 实体，不可为 {@code null}
     * @return 删除行数
     */
    int delete(T entity);

    /**
     * 删除所有实体。
     *
     * @return 删除行数
     */
    int deleteAll();
}
