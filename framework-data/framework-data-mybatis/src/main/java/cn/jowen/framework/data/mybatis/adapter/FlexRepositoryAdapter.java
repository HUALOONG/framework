package cn.jowen.framework.data.mybatis.adapter;

import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.core.repository.Repository;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@NullMarked
public final class FlexRepositoryAdapter<T, ID> {

    private static final Logger logger = LoggerFactory.getLogger(FlexRepositoryAdapter.class);

    private final Object baseMapper;
    private final Class<T> entityClass;
    private final ExtensionRegistry extensionRegistry;

    public FlexRepositoryAdapter(Object baseMapper, Class<T> entityClass, ExtensionRegistry extensionRegistry) {
        if (baseMapper == null) throw new IllegalArgumentException("baseMapper must not be null");
        this.baseMapper = baseMapper;
        if (entityClass == null) throw new IllegalArgumentException("entityClass must not be null");
        this.entityClass = entityClass;
        if (extensionRegistry == null) throw new IllegalArgumentException("extensionRegistry must not be null");
        this.extensionRegistry = extensionRegistry;
    }

    public Class<T> getEntityClass() { return entityClass; }
    public Object getBaseMapper() { return baseMapper; }

    public Optional<T> selectById(ID id) {
        try {
            T entity = invokeSelectById(id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("selectById", entityClass, e);
        }
    }

    public List<T> selectAll() {
        try {
            return invokeSelectAll();
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("selectAll", entityClass, e);
        }
    }

    public Page<T> selectAllPage(Pageable pageable) {
        return selectPage(pageable, null);
    }

    public Page<T> selectPage(Pageable pageable, @Nullable QueryWrapper<T> wrapper) {
        try {
            List<T> content = invokeSelectList(wrapper);
            long total = invokeSelectCount(wrapper);
            return new Page<>(content, total, pageable.getPage(), pageable.getSize());
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("selectPage", entityClass, e);
        }
    }

    public T insertOrUpdate(T entity) {
        try {
            ID id = invokeGetId(entity);
            if (id == null) {
                invokeInsert(entity);
            } else {
                invokeUpdateById(entity);
            }
            extensionRegistry.firePostSave(entity);
            return entity;
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("insertOrUpdate", entityClass, e);
        }
    }

    public List<T> insertAll(List<T> entities) {
        for (T entity : entities) {
            insertOrUpdate(entity);
        }
        return entities;
    }

    public int updateById(T entity) {
        try {
            extensionRegistry.firePreUpdate(entity);
            int rows = invokeUpdateByIdAndReturn(entity);
            extensionRegistry.firePostUpdate(entity);
            return rows;
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("updateById", entityClass, e);
        }
    }

    public int deleteById(ID id) {
        try { return invokeDeleteById(id); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("deleteById", entityClass, e); }
    }

    public int delete(T entity) {
        try { return invokeDeleteById(invokeGetId(entity)); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("delete", entityClass, e); }
    }

    public int deleteAll() {
        try { return invokeDeleteAll(); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("deleteAll", entityClass, e); }
    }

    public boolean existsById(ID id) {
        try { return invokeExistsById(id); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("existsById", entityClass, e); }
    }

    public long count() {
        try { return invokeCountAll(); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("count", entityClass, e); }
    }

    public long count(@Nullable QueryWrapper<T> wrapper) {
        try { return invokeSelectCount(wrapper); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("count", entityClass, e); }
    }

    // ─── Reflection calls to MyBatis Flex BaseMapper ─────────────────────

    private T invokeSelectById(ID id) throws Exception {
        try {
            var method = baseMapper.getClass().getMethod("selectById", Object.class);
            @SuppressWarnings("unchecked")
            T result = (T) method.invoke(baseMapper, id);
            return result;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> invokeSelectAll() throws Exception {
        try {
            var method = baseMapper.getClass().getMethod("selectAll");
            return (List<T>) method.invoke(baseMapper);
        } catch (NoSuchMethodException e) {
            logger.debug("BaseMapper 缺少 selectAll()，回退为空列表");
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> invokeSelectList(@Nullable QueryWrapper<T> wrapper) throws Exception {
        try {
            var method = baseMapper.getClass().getMethod("selectList", QueryWrapper.class);
            return (List<T>) method.invoke(baseMapper, wrapper);
        } catch (NoSuchMethodException e) {
            try {
                var method2 = baseMapper.getClass().getMethod("selectList");
                return (List<T>) method2.invoke(baseMapper);
            } catch (NoSuchMethodException ex) {
                return invokeSelectAll();
            }
        }
    }

    private long invokeSelectCount(@Nullable QueryWrapper<T> wrapper) throws Exception {
        try {
            var method = baseMapper.getClass().getMethod("selectCount", QueryWrapper.class);
            return (Long) method.invoke(baseMapper, wrapper);
        } catch (NoSuchMethodException e) {
            try {
                var method2 = baseMapper.getClass().getMethod("selectCount");
                return (Long) method2.invoke(baseMapper);
            } catch (NoSuchMethodException ex) {
                return invokeSelectAll().size();
            }
        }
    }

    private int invokeInsert(T entity) throws Exception {
        var method = baseMapper.getClass().getMethod("insert", Object.class);
        method.invoke(baseMapper, entity);
        return 1;
    }

    private int invokeUpdateById(T entity) throws Exception {
        int rows = invokeUpdateByIdAndReturn(entity);
        extensionRegistry.firePostUpdate(entity);
        return rows;
    }

    private int invokeUpdateByIdAndReturn(T entity) throws Exception {
        var method = baseMapper.getClass().getMethod("updateById", Object.class);
        return (Integer) method.invoke(baseMapper, entity);
    }

    private int invokeDeleteById(ID id) throws Exception {
        var method = baseMapper.getClass().getMethod("deleteById", Object.class);
        return (Integer) method.invoke(baseMapper, id);
    }

    private int invokeDeleteAll() throws Exception {
        try {
            var method = baseMapper.getClass().getMethod("deleteAll");
            return (Integer) method.invoke(baseMapper);
        } catch (NoSuchMethodException e) {
            logger.warn("BaseMapper 缺少 deleteAll()");
            return 0;
        }
    }

    private boolean invokeExistsById(ID id) throws Exception {
        try {
            var method = baseMapper.getClass().getMethod("existsById", Object.class);
            return (Boolean) method.invoke(baseMapper, id);
        } catch (NoSuchMethodException e) {
            return selectById(id).isPresent();
        }
    }

    private long invokeCountAll() throws Exception {
        try {
            var method = baseMapper.getClass().getMethod("count");
            return (Long) method.invoke(baseMapper);
        } catch (NoSuchMethodException e) {
            return invokeSelectAll().size();
        }
    }

    private ID invokeGetId(T entity) throws Exception {
        try {
            var method = entityClass.getMethod("getId");
            @SuppressWarnings("unchecked")
            ID id = (ID) method.invoke(entity);
            return id;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
