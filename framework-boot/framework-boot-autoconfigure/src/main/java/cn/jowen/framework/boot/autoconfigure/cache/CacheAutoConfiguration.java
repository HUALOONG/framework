package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
import cn.jowen.framework.cache.cache.multilevel.CacheSyncBroadcasterFactory;
import cn.jowen.framework.cache.cache.multilevel.MultilevelCacheManager;
import cn.jowen.framework.cache.cache.multilevel.RedissonCacheSyncListener;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NullMarked;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 缓存装配。向容器提供 {@link CacheManager}（默认 {@link DefaultCacheManager}）。
 *
 * <p>当 classpath 存在 Micrometer 且开启 {@code framework.cache.metrics} 时，自动将各缓存的命中率/命中数/未命中数
 * 注册为指标（{@code framework.cache.hitRate} 等），满足"多级缓存命中率可观测"的出口标准。
 *
 * <p>跨节点缓存同步：当开启 {@code framework.cache.multilevel.sync.enabled} 且 classpath 存在 Redisson 时，
 * 基于 Redisson {@code RTopic}（即 Redis pub/sub）提供 {@link CacheSyncBroadcasterFactory}，
 * 并自动附加到所有 {@link MultilevelCacheManager} Bean，使其注册的多级缓存具备跨节点一致性。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@EnableConfigurationProperties(BootCacheProperties.class)
@ConditionalOnProperty(prefix = "framework.cache", name = "enabled", matchIfMissing = true)
public class CacheAutoConfiguration {

    /**
     * 缓存管理器。用户可通过它按需创建本地/组合缓存。
     *
     * @return 缓存管理器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(BootCacheProperties properties) {
        return new DefaultCacheManager();
    }

    /**
     * 缓存指标上报器（可选）。仅当 Micrometer 在 classpath 且开启指标时生效，
     * 由 {@link CacheStatsReporter} 将各缓存命中率注册为 gauge。
     *
     * @param manager 缓存管理器，不可为 {@code null}
     * @return 指标上报器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnClass(name = "MeterRegistry")
    @ConditionalOnProperty(prefix = "framework.cache", name = "metrics", matchIfMissing = true)
    public MeterBinder cacheMetrics(CacheManager manager) {
        return new CacheStatsReporter(manager);
    }

    /**
     * 跨节点缓存同步广播器工厂（可选）。基于 Redisson {@code RTopic}（Redis pub/sub）实现，
     * 将本地写/失效广播给其他节点。默认主题前缀 {@code jowen:cache:sync:}。
     *
     * @param client Redisson 客户端，不可为 {@code null}
     * @return 广播器工厂，不可为 {@code null}
     */
    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnProperty(prefix = "framework.cache.multilevel.sync", name = "enabled", havingValue = "true")
    public CacheSyncBroadcasterFactory cacheSyncBroadcasterFactory(RedissonClient client) {
        return (cacheName, localCache) ->
                new RedissonCacheSyncListener(localCache, client, "jowen:cache:sync:" + cacheName);
    }

    /**
     * 将跨节点同步广播器工厂自动附加到所有 {@link MultilevelCacheManager} Bean，使其注册的多级缓存
     * 在本地写/失效时广播变更。无工厂 Bean 时为空操作。
     *
     * @param factoryProvider 广播器工厂（可选），不可为 {@code null}
     * @return Bean 后置处理器，不可为 {@code null}
     */
    @Bean
    public static BeanPostProcessor cacheSyncBroadcasterRegistrar(
            ObjectProvider<CacheSyncBroadcasterFactory> factoryProvider) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof MultilevelCacheManager manager
                        && factoryProvider.getIfAvailable() != null) {
                    manager.setCacheSyncBroadcasterFactory(factoryProvider.getIfAvailable());
                }
                return bean;
            }
        };
    }
}
