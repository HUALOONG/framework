package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CacheHealthIndicator} 构造期校验测试。
 *
 * <p>既有 {@code CacheHealthIndicatorTest} 全部经 {@code ApplicationContextRunner} 走正常路径，
 * 构造器的 {@code cacheManager} 非空校验（fail-fast）未触达。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class CacheHealthIndicatorGapTest {

    @SuppressWarnings("null")
    @Test
    void constructor_rejectsNullCacheManager() {
        assertThatThrownBy(() -> new CacheHealthIndicator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheManager");
    }

    @Test
    void constructor_withManager_exposesCacheCountDetail() {
        DefaultCacheManager manager = new DefaultCacheManager();
        manager.getCache("userCache");
        manager.getCache("productCache");

        Health health = new CacheHealthIndicator(manager).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("caches")).isEqualTo(2);
    }
}
