package cn.jowen.framework.extras.idempotent;

import cn.jowen.framework.extras.config.IdempotentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link IdempotentKeyInterceptor} 测试：KEY 模式首次放行/重复拒绝/异常回滚。
 */
class IdempotentKeyInterceptorTest {

    private static final AtomicInteger CALLS = new AtomicInteger();

    @BeforeEach
    void reset() {
        CALLS.set(0);
    }

    @Test
    void keyMode_firstProceeds_duplicateThrows() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(Config.class);
            ctx.refresh();
            TestService service = ctx.getBean(TestService.class);

            assertThat(service.submit("A")).isEqualTo("ok-A");
            assertThatThrownBy(() -> service.submit("A")).isInstanceOf(IdempotencyException.class);
            assertThat(CALLS.get()).isEqualTo(1);

            assertThat(service.submit("B")).isEqualTo("ok-B");
            assertThat(CALLS.get()).isEqualTo(2);
        }
    }

    @Test
    void keyMode_exceptionRollsBack_allowsRetry() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(Config.class);
            ctx.refresh();
            TestService service = ctx.getBean(TestService.class);

            assertThatThrownBy(() -> service.failing("X")).isInstanceOf(IllegalStateException.class);
            // 异常回滚键后重试成功
            assertThat(service.failing("X")).isEqualTo("done-X");
            assertThat(CALLS.get()).isEqualTo(2);
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import(IdempotentKeyInterceptor.class)
    static class Config {

        @Bean
        IdempotentValidator validator() {
            return new LocalIdempotentValidator(60_000);
        }

        @Bean
        IdempotentProperties properties() {
            return new IdempotentProperties();
        }

        @Bean
        TestService testService() {
            return new TestService();
        }
    }

    public static class TestService {

        @IdempotentKey(key = "#orderId")
        public String submit(String orderId) {
            CALLS.incrementAndGet();
            return "ok-" + orderId;
        }

        @IdempotentKey(key = "#orderId")
        public String failing(String orderId) {
            CALLS.incrementAndGet();
            if (CALLS.get() == 1) {
                throw new IllegalStateException("boom");
            }
            return "done-" + orderId;
        }
    }
}