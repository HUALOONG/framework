package cn.jowen.framework.data.core.repository;

import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.query.Query;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 条件查询仓储接口，支持按 {@link Query} 条件检索与分页。
 *
 * @param <T> 实体类型
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface QueryRepository<T> {

    /**
     * 按条件查询列表。
     *
     * @param query 查询条件，不可为 {@code null}
     * @return 实体列表
     */
    List<T> findBy(Query query);

    /**
     * 按条件分页。
     *
     * @param query   查询条件，不可为 {@code null}
     * @param pageable 分页请求，不可为 {@code null}
     * @return 分页结果
     */
    Page<T> findBy(Query query, Pageable pageable);

    /**
     * 按条件计数。
     *
     * @param query 查询条件，不可为 {@code null}
     * @return 记录数
     */
    long countBy(Query query);
}
