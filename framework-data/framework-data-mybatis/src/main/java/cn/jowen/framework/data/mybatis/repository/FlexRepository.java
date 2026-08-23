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
public class FlexRepository<T, ID> implements PagingRepository<T, ID> {

    private final FlexRepositoryAdapter<T, ID> adapter;
    private final ExtensionRegistry extensionRegistry;

    public FlexRepository(FlexRepositoryAdapter<T, ID> adapter, ExtensionRegistry extensionRegistry) {
        this.adapter = adapter;
        this.extensionRegistry = extensionRegistry;
    }

    public FlexRepositoryAdapter<T, ID> getAdapter() { return adapter; }
    public ExtensionRegistry getExtensionRegistry() { return extensionRegistry; }

    @Override public Optional<T> findById(ID id) { return adapter.selectById(id); }
    @Override public List<T> findAll() { return adapter.selectAll(); }
    @Override public Page<T> findAll(Pageable pageable) { return adapter.selectAllPage(pageable); }
    @Override public T save(T entity) { return adapter.insertOrUpdate(entity); }
    @Override public List<T> saveAll(List<T> entities) { return adapter.insertAll(entities); }
    @Override public int deleteById(ID id) { return adapter.deleteById(id); }
    @Override public boolean existsById(ID id) { return adapter.existsById(id); }
    @Override public long count() { return adapter.count(); }

    @Override
    public T update(T entity) {
        int rows = adapter.updateById(entity);
        if (rows == 0) {
            throw new cn.jowen.framework.data.mybatis.exception.FlexOptimisticLockException(
                    "更新失败: 实体不存在或版本冲突 " + adapter.getEntityClass().getSimpleName());
        }
        return entity;
    }

    @Override public int delete(T entity) { return adapter.delete(entity); }
    @Override public int deleteAll() { return adapter.deleteAll(); }
    @Override public Page<T> page(Pageable pageable) { return adapter.selectAllPage(pageable); }
    @Override public Page<T> page(Pageable pageable, QueryWrapper<T> wrapper) { return adapter.selectPage(pageable, wrapper); }
}
