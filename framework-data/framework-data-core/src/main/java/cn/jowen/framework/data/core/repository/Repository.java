package cn.jowen.framework.data.core.repository;

import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

/**
 * 通用仓储接口（CrudRepository 风格）。实现由 data-jdbc / data-mybatis 提供。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface Repository<T, ID> {

    /**
     * 按主键查询。
     *
     * @param id 主键，不可为 {@code null}
     * @return 实体（包装在 Optional）
     */
    Optional<T> findById(ID id);

    /**
     * 查询全部。
     *
     * @return 实体列表
     */
    List<T> findAll();

    /**
     * 分页查询全部。
     *
     * @param pageable 分页请求，不可为 {@code null}
     * @return 分页结果
     */
    Page<T> findAll(Pageable pageable);

    /**
     * 保存（插入或更新由实现判定：有主键则更新，无则插入）。
     *
     * @param entity 实体，不可为 {@code null}
     * @return 保存后的实体
     */
    T save(T entity);

    /**
     * 批量保存。
     *
     * @param entities 实体集合，不可为 {@code null}
     * @return 保存后的实体列表
     */
    List<T> saveAll(List<T> entities);

    /**
     * 按主键删除。
     *
     * @param id 主键，不可为 {@code null}
     * @return 删除行数
     */
    int deleteById(ID id);

    /**
     * 是否存在。
     *
     * @param id 主键，不可为 {@code null}
     * @return 存在返回 {@code true}
     */
    boolean existsById(ID id);

    /**
     * 总数。
     *
     * @return 记录数
     */
    long count();
}
