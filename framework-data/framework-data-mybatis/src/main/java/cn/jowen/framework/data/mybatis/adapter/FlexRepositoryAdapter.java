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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
/**
 * 「FlexRepositoryAdapter」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public final class FlexRepositoryAdapter<T, ID> {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(FlexRepositoryAdapter.class);

    /**
     * 反射方法句柄缓存：以声明类为键，缓存其方法名→Method 的映射，
     * 避免每次调用都执行 {@code Class.getMethod} 的反射查找开销。
     */
    private static final Map<Class<?>, Map<String, Method>> METHOD_CACHE =
            new ConcurrentHashMap<>();

    /** baseMapper 不可变字段。 */
    private final Object baseMapper;
    /** entityClass 不可变字段。 */
    private final Class<T> entityClass;
    /** extensionRegistry 不可变字段。 */
    private final ExtensionRegistry extensionRegistry;

    /**
     * 构造实例。
     * @param baseMapper 参数 baseMapper
     * @param extensionRegistry 参数 extensionRegistry
     */
    public FlexRepositoryAdapter(Object baseMapper, Class<T> entityClass, ExtensionRegistry extensionRegistry) {
        if (baseMapper == null) throw new IllegalArgumentException("baseMapper must not be null");
        this.baseMapper = baseMapper;
        if (entityClass == null) throw new IllegalArgumentException("entityClass must not be null");
        this.entityClass = entityClass;
        if (extensionRegistry == null) throw new IllegalArgumentException("extensionRegistry must not be null");
        this.extensionRegistry = extensionRegistry;
    }

    /** entityClass 字段。 */
    public Class<T> getEntityClass() { return entityClass; }
    /** baseMapper 字段。 */
    public Object getBaseMapper() { return baseMapper; }

    /**
     * 执行select by id操作。
     * @param id 参数 id
     * @return 结果
     */
    public Optional<T> selectById(ID id) {
        try {
            T entity = invokeSelectById(id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("selectById", entityClass, e);
        }
    }

    /**
     * 执行select all操作。
     * @return 结果
     */
    public List<T> selectAll() {
        try {
            return invokeSelectAll();
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("selectAll", entityClass, e);
        }
    }

    /**
     * 执行select all page操作。
     * @param pageable 参数 pageable
     * @return 结果
     */
    public Page<T> selectAllPage(Pageable pageable) {
        return selectPage(pageable, null);
    }

    /**
     * 执行select page操作。
     * @param pageable 参数 pageable
     * @return 结果
     */
    public Page<T> selectPage(Pageable pageable, @Nullable QueryWrapper<T> wrapper) {
        try {
            List<T> content = invokeSelectList(wrapper);
            long total = invokeSelectCount(wrapper);
            return new Page<>(content, total, pageable.getPage(), pageable.getSize());
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("selectPage", entityClass, e);
        }
    }

    /**
     * 执行insert or update操作。
     * @param entity 参数 entity
     * @return 结果
     */
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

    /**
     * 执行insert all操作。
     * @return 结果
     */
    public List<T> insertAll(List<T> entities) {
        for (T entity : entities) {
            insertOrUpdate(entity);
        }
        return entities;
    }

    /**
     * 执行update by id操作。
     * @param entity 参数 entity
     * @return 结果
     */
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

    /**
     * 执行delete by id操作。
     * @param id 参数 id
     * @return 结果
     */
    public int deleteById(ID id) {
        try { return invokeDeleteById(id); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("deleteById", entityClass, e); }
    }

    /**
     * 执行delete操作。
     * @param entity 参数 entity
     * @return 结果
     */
    public int delete(T entity) {
        try { return invokeDeleteById(invokeGetId(entity)); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("delete", entityClass, e); }
    }

    /**
     * 执行delete all操作。
     * @return 结果
     */
    public int deleteAll() {
        try { return invokeDeleteAll(); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("deleteAll", entityClass, e); }
    }

    /**
     * 执行exists by id操作。
     * @param id 参数 id
     * @return 结果
     */
    public boolean existsById(ID id) {
        try { return invokeExistsById(id); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("existsById", entityClass, e); }
    }

    /**
     * 执行count操作。
     * @return 结果
     */
    public long count() {
        try { return invokeCountAll(); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("count", entityClass, e); }
    }

    /**
     * 执行count操作。
     * @return 结果
     */
    public long count(@Nullable QueryWrapper<T> wrapper) {
        try { return invokeSelectCount(wrapper); }
        catch (Exception e) { throw FlexExceptionTranslator.translate("count", entityClass, e); }
    }

    // ─── Reflection calls to MyBatis Flex BaseMapper ─────────────────────

    /**
     * 从缓存获取目标类上的方法，未命中时反射查找并回填缓存。
     *
     * @param target     声明方法的类
     * @param name       方法名
     * @param paramTypes 参数类型
     * @return 找到的方法；不存在返回 {@code null}
     */
    private static @Nullable Method resolveMethod(Class<?> target, String name, Class<?>... paramTypes) {
        Map<String, Method> classMethods = METHOD_CACHE.computeIfAbsent(target, k -> new ConcurrentHashMap<>());
        String key = name + "(" + String.join(",", java.util.Arrays.toString(paramTypes)) + ")";
        Method cached = classMethods.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Method method = target.getMethod(name, paramTypes);
            classMethods.put(key, method);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private T invokeSelectById(ID id) throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "selectById", Object.class);
        if (method == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T result = (T) method.invoke(baseMapper, id);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<T> invokeSelectAll() throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "selectAll");
        if (method == null) {
            logger.debug("BaseMapper 缺少 selectAll()，回退为空列表");
            return List.of();
        }
        return (List<T>) method.invoke(baseMapper);
    }

    @SuppressWarnings("unchecked")
    private List<T> invokeSelectList(@Nullable QueryWrapper<T> wrapper) throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "selectList", QueryWrapper.class);
        if (method != null) {
            return (List<T>) method.invoke(baseMapper, wrapper);
        }
        var method2 = resolveMethod(baseMapper.getClass(), "selectList");
        if (method2 != null) {
            return (List<T>) method2.invoke(baseMapper);
        }
        return invokeSelectAll();
    }

    private long invokeSelectCount(@Nullable QueryWrapper<T> wrapper) throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "selectCount", QueryWrapper.class);
        if (method != null) {
            return (Long) method.invoke(baseMapper, wrapper);
        }
        var method2 = resolveMethod(baseMapper.getClass(), "selectCount");
        if (method2 != null) {
            return (Long) method2.invoke(baseMapper);
        }
        return invokeSelectAll().size();
    }

    private int invokeInsert(T entity) throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "insert", Object.class);
        method.invoke(baseMapper, entity);
        return 1;
    }

    private int invokeUpdateById(T entity) throws Exception {
        int rows = invokeUpdateByIdAndReturn(entity);
        extensionRegistry.firePostUpdate(entity);
        return rows;
    }

    private int invokeUpdateByIdAndReturn(T entity) throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "updateById", Object.class);
        return (Integer) method.invoke(baseMapper, entity);
    }

    private int invokeDeleteById(ID id) throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "deleteById", Object.class);
        return (Integer) method.invoke(baseMapper, id);
    }

    private int invokeDeleteAll() throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "deleteAll");
        if (method == null) {
            logger.warn("BaseMapper 缺少 deleteAll()");
            return 0;
        }
        return (Integer) method.invoke(baseMapper);
    }

    private boolean invokeExistsById(ID id) throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "existsById", Object.class);
        if (method != null) {
            return (Boolean) method.invoke(baseMapper, id);
        }
        return selectById(id).isPresent();
    }

    private long invokeCountAll() throws Exception {
        var method = resolveMethod(baseMapper.getClass(), "count");
        if (method != null) {
            return (Long) method.invoke(baseMapper);
        }
        return invokeSelectAll().size();
    }

    private ID invokeGetId(T entity) throws Exception {
        var method = resolveMethod(entityClass, "getId");
        if (method == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        ID id = (ID) method.invoke(entity);
        return id;
    }
}
