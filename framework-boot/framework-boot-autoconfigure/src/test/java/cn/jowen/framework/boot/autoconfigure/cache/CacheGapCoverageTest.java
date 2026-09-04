package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.cache.multilevel.CacheSyncBroadcaster;
import cn.jowen.framework.cache.cache.multilevel.CacheSyncBroadcasterFactory;
import cn.jowen.framework.cache.cache.multilevel.MultilevelCacheManager;
import cn.jowen.framework.cache.cache.multilevel.SyncPublishingCache;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link CacheAutoConfiguration} 与 {@link CacheAopConfiguration} 装配方法体覆盖测试。
 *
 * <p>既有 {@code CacheAutoConfigurationTest} 走条件装配验证 Bean 是否存在，
 * 但指标上报器、Redisson 广播器工厂、后置处理器注册器与 AOP 切面工厂的方法体未触达。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class CacheGapCoverageTest {

    @Test
    void cacheMetrics_returnsCacheStatsReporter() {
        assertThat(new CacheAutoConfiguration().cacheMetrics(mock(CacheManager.class)))
                .isInstanceOf(CacheStatsReporter.class);
    }

    @Test
    void cacheSyncBroadcasterFactory_createsListenerOnConfiguredTopic() {
        RTopic topic = mock(RTopic.class);
        RedissonClient client = mock(RedissonClient.class);
        when(client.getTopic("jowen:cache:sync:orders")).thenReturn(topic);

        CacheSyncBroadcasterFactory factory = new CacheAutoConfiguration()
                .cacheSyncBroadcasterFactory(client);

        assertThat(factory.create("orders", mock(Cache.class))).isNotNull();
    }

    @Test
    void cacheSyncBroadcasterRegistrar_attachesFactoryToMultilevelManager() {
        CacheSyncBroadcasterFactory factory = mock(CacheSyncBroadcasterFactory.class);
        when(factory.create(any(), any())).thenReturn(mock(CacheSyncBroadcaster.class));
        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectProvider<CacheSyncBroadcasterFactory> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(factory);

        BeanPostProcessor registrar =
                CacheAutoConfiguration.cacheSyncBroadcasterRegistrar(provider);

        MultilevelCacheManager manager = new MultilevelCacheManager();
        assertThat(registrar.postProcessBeforeInitialization(manager, "multilevelCacheManager"))
                .isSameAs(manager);
        assertThat(registrar.postProcessAfterInitialization(manager, "multilevelCacheManager"))
                .isSameAs(manager);

        // 工厂注入成功与否决定注册时是否包装为跨节点同步发布装饰
        manager.registerCache("orders", mock(Cache.class), mock(Cache.class));
        assertThat(manager.getCache("orders")).isInstanceOf(SyncPublishingCache.class);
    }

    @Test
    void cacheSyncBroadcasterRegistrar_ignoresUnrelatedBeans() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectProvider<CacheSyncBroadcasterFactory> provider = mock(ObjectProvider.class);

        BeanPostProcessor registrar =
                CacheAutoConfiguration.cacheSyncBroadcasterRegistrar(provider);
        String bean = "somePlainBean";

        assertThat(registrar.postProcessAfterInitialization(bean, bean)).isSameAs(bean);
        verifyNoInteractions(provider);
    }

    @Test
    void springCacheAnnotationProcessor_returnsAspect() {
        assertThat(new CacheAopConfiguration().springCacheAnnotationProcessor())
                .isInstanceOf(SpringCacheAnnotationProcessor.class);
    }
}
