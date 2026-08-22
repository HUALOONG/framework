package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.cache.CacheManager;
import cn.jowen.framework.extras.idempotent.Idempotent;
import cn.jowen.framework.extras.lock.LocalLock;
import cn.jowen.framework.extras.lock.Lock;
import cn.jowen.framework.extras.ratelimit.RateLimiter;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 扩展能力装配：将限流器、幂等控制、本地锁暴露为容器 bean（{@code framework.extras.enabled=false} 可整体关闭）。
 *
 * <p>幂等控制依赖 {@link CacheManager}，故声明在缓存装配之后执行。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@AutoConfiguration(afterName = "cn.jowen.framework.boot.autoconfigure.cache.CacheAutoConfiguration")
@EnableConfigurationProperties(ExtrasProperties.class)
@ConditionalOnClass(name = "cn.jowen.framework.extras.lock.Lock")
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", matchIfMissing = true)
public class ExtrasAutoConfiguration {

    /** 令牌桶限流器。 */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiter frameworkRateLimiter(ExtrasProperties properties) {
        ExtrasProperties.RateLimit rateLimit = properties.getRateLimit();
        return new RateLimiter(rateLimit.getPermitsPerSecond(), rateLimit.getCapacity());
    }

    /** 幂等控制器（令牌存储取自缓存管理器；缓存被禁用时不装配）。 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CacheManager.class)
    public Idempotent frameworkIdempotent(CacheManager cacheManager, ExtrasProperties properties) {
        ExtrasProperties.Idempotency idempotency = properties.getIdempotent();
        return new Idempotent(cacheManager.getCache(idempotency.getCacheName()), idempotency.getTtlMillis());
    }

    /** 进程内锁实现。 */
    @Bean
    @ConditionalOnMissingBean
    public Lock frameworkLocalLock() {
        return new LocalLock();
    }
}
