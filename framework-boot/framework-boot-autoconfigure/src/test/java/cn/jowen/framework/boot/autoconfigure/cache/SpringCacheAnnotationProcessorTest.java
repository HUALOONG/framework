package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.annotation.Cacheable;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
import cn.jowen.framework.cache.event.CacheEvent;
import cn.jowen.framework.cache.event.CacheEventListener;
import cn.jowen.framework.cache.event.CacheEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SpringCacheAnnotationProcessor} 测试：SpEL 键解析、缓存命中/未命中与事件分发。
 */
class SpringCacheAnnotationProcessorTest {

    private static final List<CacheEvent> EVENTS = new CopyOnWriteArrayList<>();
    private static final java.util.concurrent.atomic.AtomicInteger INVOCATIONS = new java.util.concurrent.atomic.AtomicInteger();

    @BeforeEach
    void resetState() {
        EVENTS.clear();
        INVOCATIONS.set(0);
    }

    @Test
    void cacheable_spelKey_andEventDispatch() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(TestConfig.class);
            ctx.refresh();

            TestService service = ctx.getBean(TestService.class);
            assertThat(service.fetch("a")).isEqualTo("v-a");
            assertThat(service.fetch("a")).isEqualTo("v-a");

            // 命中缓存：第二次不再执行方法
            assertThat(INVOCATIONS.get()).isEqualTo(1);

            // SpEL 键解析：#id -> "a"，命中事件携带解析后的键
            assertThat(EVENTS).anyMatch(e -> e.type() == CacheEventType.PUT && "a".equals(e.key()));
            assertThat(EVENTS).anyMatch(e -> e.type() == CacheEventType.HIT && "a".equals(e.key()));
            assertThat(EVENTS).anyMatch(e -> e.type() == CacheEventType.MISS && "a".equals(e.key()));
        }
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @Import(SpringCacheAnnotationProcessor.class)
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new DefaultCacheManager();
        }

        @Bean
        CacheEventListener listener() {
            return EVENTS::add;
        }

        @Bean
        TestService testService() {
            // 经共享计数器统计真实方法执行次数（避免 CGLIB 代理字段陷阱）
            return new TestService(INVOCATIONS);
        }
    }

    public static class TestService {
        private final java.util.concurrent.atomic.AtomicInteger calls;

        TestService(java.util.concurrent.atomic.AtomicInteger calls) {
            this.calls = calls;
        }

        @Cacheable(value = "demo", key = "#id")
        public String fetch(String id) {
            calls.incrementAndGet();
            return "v-" + id;
        }
    }
}