package cn.jowen.framework.data.core.repository;

import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import org.jspecify.annotations.NullMarked;

/**
 * 分页仓储接口，继承 {@link CrudRepository}，补充分页查询能力。
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface PagingRepository<T, ID> extends CrudRepository<T, ID> {

    /**
     * 分页查询全部数据（无查询条件）。
     *
     * @param pageable 分页请求，不可为 {@code null}
     * @return 分页结果
     */
    Page<T> page(Pageable pageable);

    /**
     * 分页查询（带查询条件）。
     *
     * @param pageable 分页请求，不可为 {@code null}
     * @param wrapper  查询条件，可为 {@code null}（等同于 page(pageable)）
     * @return 分页结果
     */
    Page<T> page(Pageable pageable, QueryWrapper<T> wrapper);
}
