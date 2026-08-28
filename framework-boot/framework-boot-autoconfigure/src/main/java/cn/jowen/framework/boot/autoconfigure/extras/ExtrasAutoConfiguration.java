package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.extras.web.ratelimit.RateLimiterManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 扩展能力装配：将限流器管理器暴露为容器 bean（{@code framework.extras.enabled=false} 可整体关闭）。
 *
 * <p>幂等控制依赖 {@link CacheManager}，故声明在缓存装配之后执行。</p>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration(afterName = "cn.jowen.framework.boot.autoconfigure.cache.CacheAutoConfiguration")
@EnableConfigurationProperties(BootExtrasProperties.class)
@ConditionalOnClass(name = "cn.jowen.framework.extras.web.lock.Lock")
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", matchIfMissing = true)
public class ExtrasAutoConfiguration {

    /**
     * 限流器实例管理器（默认令牌桶算法，内存实现；多实例部署可替换为 Redis 实现）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CacheManager.class)
    public RateLimiterManager frameworkRateLimiterManager() {
        return new RateLimiterManager();
    }
}
