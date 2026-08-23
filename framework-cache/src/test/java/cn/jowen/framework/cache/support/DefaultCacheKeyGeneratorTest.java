package cn.jowen.framework.cache.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;

/**
 * {@link DefaultCacheKeyGenerator} 测试。
 */
class DefaultCacheKeyGeneratorTest {

    private final DefaultCacheKeyGenerator generator = new DefaultCacheKeyGenerator();

    @Test
    void generate_singleArg() throws Exception {
        Method method = DefaultCacheKeyGeneratorTest.class.getMethod("sampleMethod", String.class);
        CacheOperationContext ctx = createContext("bean", method, new Object[]{"hello"}, "cache", "key");
        String key = generator.generate(ctx);
        assertThat(key).isEqualTo("arg0=hello");
    }

    @Test
    void generate_multipleArgs() throws Exception {
        Method method = DefaultCacheKeyGeneratorTest.class.getMethod("sampleMethod", String.class);
        Object[] args = {"first", "second"};
        // Use a method that accepts varargs to pass multiple args
        CacheOperationContext ctx = createContext("bean",
                DefaultCacheKeyGeneratorTest.class.getMethod("multiMethod", String.class, String.class),
                args, "cache", "key");
        String key = generator.generate(ctx);
        assertThat(key).isEqualTo("arg0=first:arg1=second");
    }

    @Test
    void generate_nullArg() throws Exception {
        Method method = DefaultCacheKeyGeneratorTest.class.getMethod("sampleMethod", String.class);
        CacheOperationContext ctx = createContext("bean", method, new Object[]{null}, "cache", "key");
        String key = generator.generate(ctx);
        assertThat(key).isEqualTo("arg0=null");
    }

    @Test
    void generate_noArgs() throws Exception {
        Method method = DefaultCacheKeyGeneratorTest.class.getMethod("noArgMethod");
        CacheOperationContext ctx = createContext("bean", method, new Object[]{}, "cache", "key");
        String key = generator.generate(ctx);
        assertThat(key).isEmpty();
    }

    @Test
    void generate_intAndStringArgs() throws Exception {
        Method method = DefaultCacheKeyGeneratorTest.class.getMethod("typedMethod", int.class, String.class);
        CacheOperationContext ctx = createContext("bean", method, new Object[]{42, "hello"}, "cache", "key");
        String key = generator.generate(ctx);
        assertThat(key).isEqualTo("arg0=42:arg1=hello");
    }

    @Test
    void generate_objectWithToString() throws Exception {
        Method method = DefaultCacheKeyGeneratorTest.class.getMethod("sampleMethod", Object.class);
        Object obj = new Object() {
            @Override
            public String toString() {
                return "custom-object";
            }
        };
        CacheOperationContext ctx = createContext("bean", method, new Object[]{obj}, "cache", "key");
        String key = generator.generate(ctx);
        assertThat(key).isEqualTo("arg0=custom-object");
    }

    private CacheOperationContext createContext(String beanName, Method method, Object[] args,
                                                 String cacheName, String key) {
        return new CacheOperationContext(beanName, method, args, new Object(),
                cacheName, key, "", "", false, "", "");
    }

    // Sample methods for test
    public void sampleMethod(String s) {}
    public void sampleMethod(Object o) {}
    public void multiMethod(String a, String b) {}
    public void noArgMethod() {}
    public void typedMethod(int i, String s) {}
}
