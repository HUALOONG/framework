package cn.jowen.framework.cache.support;

import org.jspecify.annotations.NullMarked;

/**
 * 缓存键生成器接口。支持自定义缓存键的生成逻辑。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface CacheKeyGenerator {

    /**
     * 根据方法上下文生成缓存键。
     *
     * @param context 缓存操作上下文
     * @return 缓存键，不可为 {@code null}
     */
    String generate(CacheOperationContext context);
}
