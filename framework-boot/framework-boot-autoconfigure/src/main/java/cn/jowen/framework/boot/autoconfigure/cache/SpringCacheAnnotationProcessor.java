package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.annotation.CacheAnnotationProcessor;
import cn.jowen.framework.cache.annotation.CacheEvict;
import cn.jowen.framework.cache.annotation.Cacheable;
import cn.jowen.framework.cache.annotation.CachePut;
import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.event.*;
import cn.jowen.framework.cache.support.CacheKeyGenerator;
import cn.jowen.framework.cache.support.CacheOperationContext;
import cn.jowen.framework.cache.support.DefaultCacheKeyGenerator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存注解 AOP 切面实现。拦截标注了 {@link Cacheable}、{@link CachePut}、{@link CacheEvict} 的方法，
 * 自动完成缓存读取/写入/清除，并触发缓存事件。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
@Aspect
@Component
@Order(0)
public class SpringCacheAnnotationProcessor extends CacheAnnotationProcessor {

    /**
     * keyGenerator Bean 名 -> 生成器实例映射。
     */
    private final Map<String, CacheKeyGenerator> keyGenerators = new ConcurrentHashMap<>();

    /**
     * 从 Spring 容器注入所有 CacheKeyGenerator Bean。
     */
    @Autowired(required = false)
    public void setKeyGenerators(Map<String, CacheKeyGenerator> beans) {
        if (beans != null) {
            keyGenerators.putAll(beans);
        }
    }

    /**
     * SPI：从缓存管理器获取命名缓存。
     */
    @Autowired
    private CacheManager cacheManager;

    /**
     * 处理 @Cacheable：命中缓存直接返回，未命中执行方法并缓存结果。
     */
    @Around("@annotation(cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        CacheOperationContext ctx = buildContext(joinPoint, cacheable.value(), cacheable.key(),
                cacheable.condition(), cacheable.unless(), cacheable.sync(),
                cacheable.keyGenerator(), cacheable.listener());
        return processRead(ctx);
    }

    /**
     * 处理 @CachePut：先执行方法，再将结果写入缓存。
     */
    @Around("@annotation(cachePut)")
    public Object aroundCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        CacheOperationContext ctx = buildContext(joinPoint, cachePut.value(), cachePut.key(),
                cachePut.condition(), cachePut.unless(), false,
                cachePut.keyGenerator(), cachePut.listener());
        return processWrite(ctx);
    }

    /**
     * 处理 @CacheEvict：执行方法后（或前）清除缓存。
     */
    @Around("@annotation(cacheEvict)")
    public Object aroundCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        CacheOperationContext ctx = buildContext(joinPoint, cacheEvict.value(), cacheEvict.key(),
                "", "", false,
                cacheEvict.keyGenerator(), cacheEvict.listener());
        if (cacheEvict.beforeInvocation()) {
            evict(ctx);
        }
        Object result = joinPoint.proceed();
        if (!cacheEvict.beforeInvocation()) {
            evict(ctx);
        }
        return result;
    }

    private CacheOperationContext buildContext(ProceedingJoinPoint joinPoint,
                                                String cacheName, String key,
                                                String condition, String unless,
                                                boolean sync, String keyGeneratorName,
                                                String listenerName) {
        Object target = joinPoint.getTarget();
        Class<?> targetClass = target.getClass();
        Method method = resolveMethod(joinPoint, targetClass);
        Object[] args = joinPoint.getArgs();
        String beanName = (target instanceof BeanNameAware) ? target.toString() : targetClass.getSimpleName();
        return new CacheOperationContext(beanName, method, args, target,
                cacheName, key, condition, unless, sync, keyGeneratorName, listenerName);
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint, Class<?> targetClass) {
        Object[] args = joinPoint.getArgs();
        Class<?>[] paramTypes = args == null ? new Class<?>[0] : resolveParameterTypes(joinPoint);
        try {
            return targetClass.getMethod(joinPoint.getSignature().getName(), paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Class<?>[] resolveParameterTypes(ProceedingJoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature ms) {
            return ms.getParameterTypes();
        }
        return new Class<?>[0];
    }

    @SuppressWarnings("unchecked")
    private <T> T getTypedCache(CacheManager cacheManager, String cacheName) {
        return (T) cacheManager.getCache(cacheName);
    }

    private Object processRead(CacheOperationContext ctx) throws Throwable {
        CacheKeyGenerator gen = resolveKeyGenerator(ctx.keyGenerator());
        String key = ctx.key().isEmpty() ? gen.generate(ctx) : evaluateSpEL(ctx.key(), ctx);
        Cache<String, Object> cache = getTypedCache(cacheManager, ctx.cacheName());

        // 命中缓存直接返回
        Object cached = cache.get(key);
        if (cached != null) {
            publishEvent(new CacheHitEvent(ctx.cacheName(), key, java.time.Instant.now()));
            return cached;
        }
        publishEvent(new CacheMissEvent(ctx.cacheName(), key, java.time.Instant.now()));

        // 执行方法
        Object result;
        if (ctx.isSync()) {
            result = synchronizeLoad(ctx, key, cache);
        } else {
            result = executeMethod(ctx);
        }

        // 写入缓存
        if (result != null) {
            cache.put(key, result);
            publishEvent(new CachePutEvent(ctx.cacheName(), key, java.time.Instant.now(), 0L));
        }
        return result;
    }

    private Object processWrite(CacheOperationContext ctx) throws Throwable {
        CacheKeyGenerator gen = resolveKeyGenerator(ctx.keyGenerator());
        String key = ctx.key().isEmpty() ? gen.generate(ctx) : evaluateSpEL(ctx.key(), ctx);
        Cache<String, Object> cache = getTypedCache(cacheManager, ctx.cacheName());

        Object result = executeMethod(ctx);
        if (result != null) {
            cache.put(key, result);
            publishEvent(new CachePutEvent(ctx.cacheName(), key, java.time.Instant.now(), 0L));
        }
        return result;
    }

    private void evict(CacheOperationContext ctx) {
        CacheKeyGenerator gen = resolveKeyGenerator(ctx.keyGenerator());
        String key = ctx.key().isEmpty() ? gen.generate(ctx) : evaluateSpEL(ctx.key(), ctx);
        Cache<String, Object> cache = getTypedCache(cacheManager, ctx.cacheName());
        cache.evict(key);
        publishEvent(new CacheEvictEvent(ctx.cacheName(), key, java.time.Instant.now()));
    }

    private Object executeMethod(CacheOperationContext ctx) throws Throwable {
        return ctx.target().getClass().getMethod(ctx.method().getName(), ctx.method().getParameterTypes())
                .invoke(ctx.target(), ctx.args());
    }

    private Object synchronizeLoad(CacheOperationContext ctx, String key, Cache<String, Object> cache) throws Throwable {
        synchronized (cache) {
            Object cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
            Object result = executeMethod(ctx);
            if (result != null) {
                cache.put(key, result);
                publishEvent(new CachePutEvent(ctx.cacheName(), key, java.time.Instant.now(), 0L));
            }
            return result;
        }
    }

    private CacheKeyGenerator resolveKeyGenerator(String beanName) {
        if (beanName == null || beanName.isEmpty()) {
            return new DefaultCacheKeyGenerator();
        }
        return keyGenerators.computeIfAbsent(beanName, name -> {
            throw new IllegalArgumentException("CacheKeyGenerator bean not found: " + name);
        });
    }

    private String evaluateSpEL(String expression, CacheOperationContext ctx) {
        // TODO: 接入 SpEL 表达式解析（Spring Expression）
        return expression;
    }

    private void publishEvent(CacheEvent event) {
        // TODO: 通过 EventBus SPI 异步分发
    }

    @Override
    public void processCacheable(CacheOperationContext context, CacheManager cacheManager, @Nullable CacheKeyGenerator keyGenerator) {

    }

    @Override
    public void processCachePut(CacheOperationContext context, CacheManager cacheManager, @Nullable CacheKeyGenerator keyGenerator) {

    }

    @Override
    public void processCacheEvict(CacheOperationContext context, CacheManager cacheManager, @Nullable CacheKeyGenerator keyGenerator) {

    }
}
