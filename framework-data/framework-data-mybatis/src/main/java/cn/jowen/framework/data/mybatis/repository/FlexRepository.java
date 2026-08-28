package cn.jowen.framework.data.mybatis.repository;

import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.core.repository.CrudRepository;
import cn.jowen.framework.data.core.repository.PagingRepository;
import cn.jowen.framework.data.mybatis.adapter.FlexRepositoryAdapter;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

@NullMarked
/**
 * 「FlexRepository」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexRepository<T, ID> implements PagingRepository<T, ID> {

    /** adapter 不可变字段。 */
    private final FlexRepositoryAdapter<T, ID> adapter;
    /** extensionRegistry 不可变字段。 */
    private final ExtensionRegistry extensionRegistry;

    /**
     * 构造实例。
     * @param extensionRegistry 参数 extensionRegistry
     */
    public FlexRepository(FlexRepositoryAdapter<T, ID> adapter, ExtensionRegistry extensionRegistry) {
        this.adapter = adapter;
        this.extensionRegistry = extensionRegistry;
    }

    /** adapter 字段。 */
    public FlexRepositoryAdapter<T, ID> getAdapter() { return adapter; }
    /** extensionRegistry 字段。 */
    public ExtensionRegistry getExtensionRegistry() { return extensionRegistry; }

    /** return 字段。 */
    @Override public Optional<T> findById(ID id) { return adapter.selectById(id); }
    /** return 字段。 */
    @Override public List<T> findAll() { return adapter.selectAll(); }
    /** return 字段。 */
    @Override public Page<T> findAll(Pageable pageable) { return adapter.selectAllPage(pageable); }
    /** return 字段。 */
    @Override public T save(T entity) { return adapter.insertOrUpdate(entity); }
    /** return 字段。 */
    @Override public List<T> saveAll(List<T> entities) { return adapter.insertAll(entities); }
    /** return 字段。 */
    @Override public int deleteById(ID id) { return adapter.deleteById(id); }
    /** return 字段。 */
    @Override public boolean existsById(ID id) { return adapter.existsById(id); }
    /** return 字段。 */
    @Override public long count() { return adapter.count(); }

    /**
     * 执行update操作。
     * @param entity 参数 entity
     * @return 结果
     */
    @Override
    public T update(T entity) {
        int rows = adapter.updateById(entity);
        if (rows == 0) {
            throw new cn.jowen.framework.data.mybatis.exception.FlexOptimisticLockException(
                    "更新失败: 实体不存在或版本冲突 " + adapter.getEntityClass().getSimpleName());
        }
        return entity;
    }

    /** return 字段。 */
    @Override public int delete(T entity) { return adapter.delete(entity); }
    /** return 字段。 */
    @Override public int deleteAll() { return adapter.deleteAll(); }
    /** return 字段。 */
    @Override public Page<T> page(Pageable pageable) { return adapter.selectAllPage(pageable); }
    /** return 字段。 */
    @Override public Page<T> page(Pageable pageable, QueryWrapper<T> wrapper) { return adapter.selectPage(pageable, wrapper); }
}
