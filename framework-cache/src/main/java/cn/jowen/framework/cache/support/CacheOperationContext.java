package cn.jowen.framework.cache.support;

import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Method;

/**
 * 缓存操作上下文。封装方法调用信息，用于缓存键生成和条件解析。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class CacheOperationContext {

    private final String beanName;
    private final Method method;
    private final Object[] args;
    private final Object target;
    private final String cacheName;
    private final String key;
    private final String condition;
    private final String unless;
    private final boolean sync;
    private final String keyGenerator;
    private final String listener;

    public CacheOperationContext(String beanName, Method method, Object[] args, Object target,
                                  String cacheName, String key, String condition, String unless,
                                  boolean sync, String keyGenerator, String listener) {
        this.beanName = beanName;
        this.method = method;
        this.args = args;
        this.target = target;
        this.cacheName = cacheName;
        this.key = key;
        this.condition = condition;
        this.unless = unless;
        this.sync = sync;
        this.keyGenerator = keyGenerator;
        this.listener = listener;
    }

    public String beanName() {
        return beanName;
    }

    public Method method() {
        return method;
    }

    public Object[] args() {
        return args;
    }

    public Object target() {
        return target;
    }

    public String cacheName() {
        return cacheName;
    }

    public String key() {
        return key;
    }

    public String condition() {
        return condition;
    }

    public String unless() {
        return unless;
    }

    public boolean isSync() {
        return sync;
    }

    public String keyGenerator() {
        return keyGenerator;
    }

    public String listener() {
        return listener;
    }
}
