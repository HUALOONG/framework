package cn.jowen.framework.cache.annotation;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.support.CacheKeyGenerator;
import cn.jowen.framework.cache.support.CacheOperationContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * 缓存注解处理器抽象基类。子类实现具体的 AOP 切面逻辑，
 * 通过 SPI 机制在 boot-autoconfigure 模块中装配。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class CacheAnnotationProcessor {

    /**
     * 处理 @Cacheable 注解。
     *
     * @param context 缓存操作上下文，不可为 {@code null}
     * @param cacheManager 缓存管理器，不可为 {@code null}
     * @param keyGenerator 缓存键生成器，可能为 {@code null}
     */
    public abstract void processCacheable(CacheOperationContext context,
                                          CacheManager cacheManager,
                                          @Nullable CacheKeyGenerator keyGenerator);

    /**
     * 处理 @CachePut 注解。
     *
     * @param context 缓存操作上下文，不可为 {@code null}
     * @param cacheManager 缓存管理器，不可为 {@code null}
     * @param keyGenerator 缓存键生成器，可能为 {@code null}
     */
    public abstract void processCachePut(CacheOperationContext context,
                                         CacheManager cacheManager,
                                         @Nullable CacheKeyGenerator keyGenerator);

    /**
     * 处理 @CacheEvict 注解。
     *
     * @param context 缓存操作上下文，不可为 {@code null}
     * @param cacheManager 缓存管理器，不可为 {@code null}
     * @param keyGenerator 缓存键生成器，可能为 {@code null}
     */
    public abstract void processCacheEvict(CacheOperationContext context,
                                           CacheManager cacheManager,
                                           @Nullable CacheKeyGenerator keyGenerator);

    /**
     * 解析缓存操作上下文。
     *
     * @param beanName Bean 名称
     * @param method 方法对象
     * @param args 方法参数
     * @param target 目标对象
     * @return 缓存操作上下文，不可为 {@code null}
     */
    protected CacheOperationContext createContext(String beanName, Method method,
                                                   Object[] args, Object target) {
        return new CacheOperationContext(beanName, method, args, target,
                "", "", "", "", false, "", "");
    }
}
