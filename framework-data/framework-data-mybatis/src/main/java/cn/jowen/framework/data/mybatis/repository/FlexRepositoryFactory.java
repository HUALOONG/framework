package cn.jowen.framework.data.mybatis.repository;

import cn.jowen.framework.data.core.repository.Repository;
import cn.jowen.framework.data.core.repository.RepositoryFactory;
import cn.jowen.framework.data.mybatis.adapter.FlexRepositoryAdapter;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@NullMarked
/**
 * 「FlexRepository」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexRepositoryFactory implements RepositoryFactory {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(FlexRepositoryFactory.class);

    /** extensionRegistry 不可变字段。 */
    private final ExtensionRegistry extensionRegistry;
    /** flexFactory 不可变字段。 */
    private final Object flexFactory;
    /** cache 不可变字段。 */
    private final ConcurrentMap<Class<?>, Repository<?, ?>> cache = new ConcurrentHashMap<>();

    /**
     * 构造实例。
     * @param flexFactory 参数 flexFactory
     * @param extensionRegistry 参数 extensionRegistry
     */
    public FlexRepositoryFactory(Object flexFactory, ExtensionRegistry extensionRegistry) {
        this.flexFactory = flexFactory;
        this.extensionRegistry = extensionRegistry;
    }

    /**
     * 执行@ suppress warnings操作。
     * @return 结果
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <T, ID> Repository<T, ID> getRepository(Class<T> entityClass) {
        return (Repository<T, ID>) cache.computeIfAbsent(entityClass, clazz -> {
            FlexRepositoryAdapter<T, ID> adapter = (FlexRepositoryAdapter<T, ID>) createAdapter(clazz);
            FlexRepository<T, ID> repository = new FlexRepository<>(adapter, extensionRegistry);
            logger.debug("创建 FlexRepository: " + clazz.getSimpleName());
            return repository;
        });
    }

    /**
     * 执行@ suppress warnings操作。
     * @return 结果
     */
    @SuppressWarnings("unchecked")
    public <T> FlexJoinRepository<T> getJoinRepository(Class<T> entityClass) {
        FlexRepositoryAdapter<T, ?> adapter = (FlexRepositoryAdapter<T, ?>) createAdapter(entityClass);
        return new FlexJoinRepository<>(adapter);
    }

    /**
     * 获取dynamic repository。
     * @return 结果
     */
    public FlexDynamicRepository getDynamicRepository() {
        return new FlexDynamicRepository(flexFactory);
    }

    private FlexRepositoryAdapter<?, ?> createAdapter(Class<?> entityClass) {
        try {
            var method = flexFactory.getClass().getMethod("getMapper", Class.class);
            Object baseMapper = method.invoke(flexFactory, entityClass);
            if (baseMapper != null) {
                return new FlexRepositoryAdapter<>(baseMapper, entityClass, extensionRegistry);
            }
        } catch (Exception e) {
            logger.warn("获取 BaseMapper 失败 (" + entityClass.getSimpleName() + "): " + e.getMessage());
        }
        // Fallback: no-op proxy
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                entityClass.getClassLoader(),
                new Class<?>[]{BaseMapper.class},
                (p, m, args) -> {
                    if (m.getName().equals("hashCode")) return 1;
                    if (m.getName().equals("toString")) return "BaseMapper[" + entityClass.getSimpleName() + "]";
                    return null;
                });
        return new FlexRepositoryAdapter<>(proxy, entityClass, extensionRegistry);
    }

    /**
     * 「BaseMapper」接口定义。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public interface BaseMapper<T> {}
}
