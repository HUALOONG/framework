package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import cn.jowen.framework.extras.idempotent.Idempotent;
import cn.jowen.framework.extras.lock.Lock;
import cn.jowen.framework.extras.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * extras 装配集成测试：验证限流器/幂等/本地锁 bean 注册、属性绑定与禁用开关。
 */
class ExtrasAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BootAutoConfiguration.class));

    @Test
    void extrasBeansRegistered() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RateLimiter.class);
            assertThat(context).hasSingleBean(Idempotent.class);
            assertThat(context).hasSingleBean(Lock.class);
        });
    }

    @Test
    void rateLimitPropertiesBound() {
        runner.withPropertyValues(
                "framework.extras.rate-limit.permits-per-second=2",
                "framework.extras.rate-limit.capacity=5").run(context -> {
            RateLimiter limiter = context.getBean(RateLimiter.class);
            // 容量 5：前 5 次放行，第 6 次拒绝（速率 2/s 来不及补充）
            for (int i = 0; i < 5; i++) {
                assertThat(limiter.tryAcquire()).isTrue();
            }
            assertThat(limiter.tryAcquire()).isFalse();
        });
    }

    @Test
    void extrasDisabledByProperty() {
        runner.withPropertyValues("framework.extras.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(RateLimiter.class);
            assertThat(context).doesNotHaveBean(Idempotent.class);
            assertThat(context).doesNotHaveBean(Lock.class);
        });
    }
}
