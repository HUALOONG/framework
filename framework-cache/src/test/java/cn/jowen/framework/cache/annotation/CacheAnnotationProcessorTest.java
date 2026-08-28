package cn.jowen.framework.cache.annotation;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.support.CacheKeyGenerator;
import cn.jowen.framework.cache.support.CacheOperationContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheAnnotationProcessor} 单元测试。
 *
 * <p>该类为抽象基类，唯一可执行生产代码是其受保护方法 {@code createContext}。
 * 通过定义具体子类（空实现三个抽象切面方法）覆盖 {@code createContext} 的字段装配逻辑，
 * 并额外以反射读取 @Cacheable / @CachePut / @CacheEvict 注解，验证注解在运行期可读。
 * 全程无 Mockito、无 Spring 容器依赖。
 */
class CacheAnnotationProcessorTest {

    /** 仅用于实例化并调用受保护 {@code createContext} 的具体子类。 */
    static final class TestProcessor extends CacheAnnotationProcessor {
        @Override
        public void processCacheable(CacheOperationContext context, CacheManager cacheManager,
                                      CacheKeyGenerator keyGenerator) {
        }

        @Override
        public void processCachePut(CacheOperationContext context, CacheManager cacheManager,
                                     CacheKeyGenerator keyGenerator) {
        }

        @Override
        public void processCacheEvict(CacheOperationContext context, CacheManager cacheManager,
                                       CacheKeyGenerator keyGenerator) {
        }
    }

    @Test
    void createContext_populatesContextFields() throws Exception {
        TestProcessor processor = new TestProcessor();
        Method method = CacheAnnotationProcessorTest.class.getMethod("targetMethod");
        CacheOperationContext ctx = processor.createContext("bean", method, new Object[] {"a", 1}, "target");

        assertThat(ctx.beanName()).isEqualTo("bean");
        assertThat(ctx.method()).isEqualTo(method);
        assertThat(ctx.args()).containsExactly("a", 1);
        assertThat(ctx.target()).isEqualTo("target");
        assertThat(ctx.cacheName()).isEmpty();
        assertThat(ctx.key()).isEmpty();
        assertThat(ctx.condition()).isEmpty();
        assertThat(ctx.unless()).isEmpty();
        assertThat(ctx.isSync()).isFalse();
        assertThat(ctx.keyGenerator()).isEmpty();
        assertThat(ctx.listener()).isEmpty();
    }

    @Test
    void annotations_readableViaReflection() throws Exception {
        Method cached = AnnotatedTarget.class.getMethod("cached");
        assertThat(cached.isAnnotationPresent(Cacheable.class)).isTrue();
        assertThat(cached.getAnnotation(Cacheable.class).value()).isEqualTo("users");

        Method put = AnnotatedTarget.class.getMethod("put");
        assertThat(put.isAnnotationPresent(CachePut.class)).isTrue();

        Method evict = AnnotatedTarget.class.getMethod("evict");
        assertThat(evict.isAnnotationPresent(CacheEvict.class)).isTrue();
    }

    public void targetMethod() {
    }

    static class AnnotatedTarget {
        @Cacheable("users")
        public String cached() {
            return "";
        }

        @CachePut("users")
        public void put() {
        }

        @CacheEvict("users")
        public void evict() {
        }
    }
}
