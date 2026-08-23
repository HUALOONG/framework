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
public class FlexRepositoryFactory implements RepositoryFactory {

    private static final Logger logger = LoggerFactory.getLogger(FlexRepositoryFactory.class);

    private final ExtensionRegistry extensionRegistry;
    private final Object flexFactory;
    private final ConcurrentMap<Class<?>, Repository<?, ?>> cache = new ConcurrentHashMap<>();

    public FlexRepositoryFactory(Object flexFactory, ExtensionRegistry extensionRegistry) {
        this.flexFactory = flexFactory;
        this.extensionRegistry = extensionRegistry;
    }

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

    @SuppressWarnings("unchecked")
    public <T> FlexJoinRepository<T> getJoinRepository(Class<T> entityClass) {
        FlexRepositoryAdapter<T, ?> adapter = (FlexRepositoryAdapter<T, ?>) createAdapter(entityClass);
        return new FlexJoinRepository<>(adapter);
    }

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

    public interface BaseMapper<T> {}
}
