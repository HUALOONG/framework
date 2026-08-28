package cn.jowen.framework.demo.config;

import cn.jowen.framework.cache.api.CacheConfiguration;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.cache.caffeine.CaffeineCache;
import cn.jowen.framework.demo.entity.User;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 缓存定制：演示如何覆盖框架的默认缓存参数。
 *
 * <p>{@link CacheManager#getCache(String)} 在缓存缺失时会用<b>默认配置</b>自动创建，
 * 若业务需要指定容量、过期时间、空值缓存或互斥回源，则像下面这样显式注册同名缓存即可。
 *
 * <p><b>扩展提示</b>：
 * <ul>
 *   <li>换成 Redis —— 注册 {@code RedissonCache.create(name, config)} 并引入 redisson 依赖；</li>
 *   <li>多级缓存 —— 使用 {@code MultilevelCache}，本地 Caffeine + 远端 Redis 组合；</li>
 *   <li>序列化 —— 通过 {@code CacheConfiguration#serializer} 指定 Jackson/Kryo。</li>
 * </ul>
 *
 * @author demo
 * @since 0.0.1
 * @version 0.0.1
 */
@Configuration
public class DemoCacheConfiguration {

    /** USER_CACHE 常量。 */
    private static final String USER_CACHE = "demo:user";

    /** cacheManager 不可变字段。 */
    private final CacheManager cacheManager;

    /**
     * 构造实例。
     * @param cacheManager 参数 cacheManager
     */
    public DemoCacheConfiguration(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 注册自定义参数的用户缓存。
     */
    @PostConstruct
    public void registerUserCache() {
        CacheConfiguration config = CacheConfiguration.builder()
                .maxSize(1_000L)                              // 最多缓存 1000 条
                .expireAfterWrite(Duration.ofMinutes(10))     // 写入 10 分钟后过期
                .expireAfterAccess(Duration.ofMinutes(5))     // 5 分钟未访问过期
                .build();
        cacheManager.register(CaffeineCache.<Long, User>create(USER_CACHE, config));
    }
}
