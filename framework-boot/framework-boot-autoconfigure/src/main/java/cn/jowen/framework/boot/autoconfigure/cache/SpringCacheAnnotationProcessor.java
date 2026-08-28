package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.annotation.CacheAnnotationProcessor;
import cn.jowen.framework.cache.annotation.CacheEvict;
import cn.jowen.framework.cache.annotation.Cacheable;
import cn.jowen.framework.cache.annotation.CachePut;
import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.event.CacheEventListener;
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
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
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
     * 缓存事件监听器（无 EventBus，直接逐个分发）。
     */
    private final List<CacheEventListener> eventListeners = new java.util.ArrayList<>();

    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

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
     * 从 Spring 容器注入所有缓存事件监听器。
     */
    @Autowired(required = false)
    public void setEventListeners(List<CacheEventListener> listeners) {
        if (listeners != null) {
            eventListeners.addAll(listeners);
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
        if (expression == null || expression.isBlank()) {
            return expression;
        }
        StandardEvaluationContext eval = new StandardEvaluationContext();
        eval.setVariable("target", ctx.target());
        Object[] args = ctx.args() == null ? new Object[0] : ctx.args();
        Method method = ctx.method();
        String[] names = method == null ? null : PARAMETER_NAMES.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            // 支持 #a0/#p0 索引引用与按参数名引用（如 #id）
            eval.setVariable("a" + i, args[i]);
            eval.setVariable("p" + i, args[i]);
            if (names != null && i < names.length && names[i] != null) {
                eval.setVariable(names[i], args[i]);
            }
        }
        return String.valueOf(SPEL_PARSER.parseExpression(expression).getValue(eval));
    }

    private void publishEvent(CacheEvent event) {
        // cache 模块无 EventBus，直接逐个分发；单监听失败不阻断缓存主流程
        for (CacheEventListener listener : eventListeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ignored) {
                // 事件监听属旁路能力，失败仅忽略
            }
        }
    }

    @Override
    public void processCacheable(CacheOperationContext context, CacheManager cacheManager, @Nullable CacheKeyGenerator keyGenerator) {
        // 基类 SPI 签名为 void、无法返回方法结果，故委托既有的读-写缓存实现
        try {
            processRead(context);
        } catch (Throwable e) {
            throw new IllegalStateException("缓存读取失败", e);
        }
    }

    @Override
    public void processCachePut(CacheOperationContext context, CacheManager cacheManager, @Nullable CacheKeyGenerator keyGenerator) {
        try {
            processWrite(context);
        } catch (Throwable e) {
            throw new IllegalStateException("缓存写入失败", e);
        }
    }

    @Override
    public void processCacheEvict(CacheOperationContext context, CacheManager cacheManager, @Nullable CacheKeyGenerator keyGenerator) {
        evict(context);
    }
}
