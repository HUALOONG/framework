package cn.jowen.framework.cache.cache.caffeine;

import org.jspecify.annotations.NullMarked;

/**
 * Caffeine 构建器配置器。
 * <p>参数为 {@code Caffeine.newBuilder()} 返回的 Builder 实例（调用方可强转为具体类型使用）。
 * <pre>{@code
 *   CaffeineCacheManager manager = new CaffeineCacheManager();
 *   manager.configure(b -> {
 *       var builder = (com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
 *               .maximumSize(1000));
 *       // configure as needed
 *   });
 * }</pre>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@FunctionalInterface
@NullMarked
public interface CaffeineConfigurer {

    void configure(Object builder);
}
