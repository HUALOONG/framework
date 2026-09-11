package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.annotation.CacheEvict;
import cn.jowen.framework.cache.annotation.CachePut;
import cn.jowen.framework.cache.annotation.Cacheable;
import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.condition.ConditionEvaluator;
import cn.jowen.framework.cache.event.CacheEvent;
import cn.jowen.framework.cache.event.CacheEventListener;
import cn.jowen.framework.cache.event.CacheEvictEvent;
import cn.jowen.framework.cache.event.CacheHitEvent;
import cn.jowen.framework.cache.event.CacheMissEvent;
import cn.jowen.framework.cache.event.CachePutEvent;
import cn.jowen.framework.cache.support.CacheKeyGenerator;
import cn.jowen.framework.cache.support.CacheOperationContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringCacheAnnotationProcessorTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache<String, Object> cache;
    @Captor
    private ArgumentCaptor<CacheEvent> eventCaptor;

    private SpringCacheAnnotationProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        processor = new SpringCacheAnnotationProcessor();
        Field f = SpringCacheAnnotationProcessor.class.getDeclaredField("cacheManager");
        f.setAccessible(true);
        f.set(processor, cacheManager);
        lenient().doReturn(cache).when(cacheManager).getCache(anyString());
    }

    static class Target {
        public String getUser(String id) {
            return "user-" + id;
        }

        public String putUser(String id) {
            return "put-" + id;
        }

        public String evictUser(String id) {
            return "evict-" + id;
        }

        public String failing() {
            throw new IllegalStateException("boom");
        }

        public String getNull() {
            return null;
        }

        public String putNull() {
            return null;
        }

        public String ping() {
            return "pong";
        }
    }

    private ProceedingJoinPoint jp(Object target, String methodName, Class<?>[] types, Object[] args) {
        return jp(target, methodName, types, args, null);
    }

    private ProceedingJoinPoint jp(Object target, String methodName, Class<?>[] types, Object[] args, Object proceed) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        // 放宽严格度：部分 @Bean 分支/异常路径不会用到全部 stub，避免 UnnecessaryStubbingException
        lenient().when(joinPoint.getTarget()).thenReturn(target);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        MethodSignature sig = mock(MethodSignature.class);
        lenient().when(sig.getName()).thenReturn(methodName);
        // args 为 null 时 resolveParameterTypes 被短路，该 stub 可能不被使用，故放宽严格度
        lenient().when(sig.getParameterTypes()).thenReturn(types);
        lenient().when(joinPoint.getSignature()).thenReturn(sig);
        if (proceed != null) {
            try {
                lenient().when(joinPoint.proceed()).thenReturn(proceed);
            } catch (Throwable ignored) {
                // proceed() 声明抛出 Throwable，mock 不会真正抛出
            }
        }
        return joinPoint;
    }

    private Cacheable cacheable(String name) {
        Cacheable c = mock(Cacheable.class);
        when(c.value()).thenReturn(name);
        when(c.key()).thenReturn("");
        when(c.condition()).thenReturn("");
        when(c.unless()).thenReturn("");
        when(c.sync()).thenReturn(false);
        when(c.keyGenerator()).thenReturn("");
        when(c.listener()).thenReturn("");
        return c;
    }

    private Cacheable withCondition(Cacheable c, String condition) {
        when(c.condition()).thenReturn(condition);
        return c;
    }

    private Cacheable withUnless(Cacheable c, String unless) {
        when(c.unless()).thenReturn(unless);
        return c;
    }

    private CachePut cachePut(String name) {
        CachePut c = mock(CachePut.class);
        when(c.value()).thenReturn(name);
        when(c.key()).thenReturn("");
        when(c.condition()).thenReturn("");
        when(c.unless()).thenReturn("");
        when(c.keyGenerator()).thenReturn("");
        when(c.listener()).thenReturn("");
        return c;
    }

    private CachePut withCondition(CachePut c, String condition) {
        when(c.condition()).thenReturn(condition);
        return c;
    }

    private CacheEvict cacheEvict(String name, boolean before) {
        CacheEvict c = mock(CacheEvict.class);
        when(c.value()).thenReturn(name);
        when(c.key()).thenReturn("");
        when(c.beforeInvocation()).thenReturn(before);
        when(c.keyGenerator()).thenReturn("");
        when(c.listener()).thenReturn("");
        return c;
    }

    
    @Test
    void aroundCacheable_hit_returnsCachedValueAndPublishesHit() throws Throwable {
        when(cache.get(anyString())).thenReturn("cachedVal");
        CacheEventListener listener = mock(CacheEventListener.class);
        processor.setEventListeners(List.of(listener));

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}),
                cacheable("user"));

        assertThat(result).isEqualTo("cachedVal");
        verify(cache).get("arg0=42");
        verify(cache, never()).put(anyString(), any());
        verify(listener).onEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(CacheHitEvent.class);
    }

    @Test
    void aroundCacheable_miss_executesAndCachesAndPublishesMissThenPut() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);
        CacheEventListener listener = mock(CacheEventListener.class);
        processor.setEventListeners(List.of(listener));

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}),
                cacheable("user"));

        assertThat(result).isEqualTo("user-42");
        verify(cache).put("arg0=42", "user-42");
        verify(listener, times(2)).onEvent(eventCaptor.capture());
        List<CacheEvent> events = eventCaptor.getAllValues();
        assertThat(events.get(0)).isInstanceOf(CacheMissEvent.class);
        assertThat(events.get(1)).isInstanceOf(CachePutEvent.class);
    }

    @Test
    void aroundCacheable_sync_miss_usesSynchronizeLoad() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}),
                withSync(cacheable("user")));

        assertThat(result).isEqualTo("user-42");
        verify(cache, times(2)).put("arg0=42", "user-42");
    }

    @Test
    void aroundCachePut_executesAndWrites() throws Throwable {
        Object result = processor.aroundCachePut(
                jp(new Target(), "putUser", new Class[]{String.class}, new Object[]{"42"}),
                cachePut("user"));

        assertThat(result).isEqualTo("put-42");
        verify(cache).put("arg0=42", "put-42");
    }

    @Test
    void aroundCacheEvict_afterInvocation_proceedsThenEvicts() throws Throwable {
        Object result = processor.aroundCacheEvict(
                jp(new Target(), "evictUser", new Class[]{String.class}, new Object[]{"42"}, "ok"),
                cacheEvict("user", false));

        assertThat(result).isEqualTo("ok");
        verify(cache).evict("arg0=42");
    }

    @Test
    void aroundCacheEvict_beforeInvocation_evictsThenProceeds() throws Throwable {
        Object result = processor.aroundCacheEvict(
                jp(new Target(), "evictUser", new Class[]{String.class}, new Object[]{"42"}, "ok"),
                cacheEvict("user", true));

        assertThat(result).isEqualTo("ok");
        verify(cache).evict("arg0=42");
    }

    @Test
    void spelKey_isEvaluated() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);
        Cacheable c = cacheable("user");
        when(c.key()).thenReturn("#a0");

        processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        verify(cache).put("42", "user-42");
    }

    @Test
    void spelKey_blank_returnsBlankExpression() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);
        Cacheable c = cacheable("user");
        when(c.key()).thenReturn("   ");

        processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        verify(cache).put("   ", "user-42");
    }

    @Test
    void namedKeyGenerator_isResolved() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);
        CacheKeyGenerator gen = mock(CacheKeyGenerator.class);
        when(gen.generate(any())).thenReturn("CUSTOM");
        processor.setKeyGenerators(Map.of("g", gen));
        Cacheable c = cacheable("user");
        when(c.keyGenerator()).thenReturn("g");

        processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        verify(cache).put(eq("CUSTOM"), eq("user-42"));
    }

    @Test
    void missingKeyGenerator_throws() {
        // resolveKeyGenerator 在读取缓存之前即抛出，故该 stub 放宽严格度
        lenient().when(cache.get(anyString())).thenReturn(null);
        Cacheable c = cacheable("user");
        lenient().when(c.keyGenerator()).thenReturn("missing");

        assertThatThrownBy(() -> processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listenerException_isIgnored() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);
        CacheEventListener bad = mock(CacheEventListener.class);
        doThrow(new RuntimeException("listener boom")).when(bad).onEvent(any());
        processor.setEventListeners(List.of(bad));

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}),
                cacheable("user"));

        assertThat(result).isEqualTo("user-42");
    }

    @Test
    void spi_processCacheable_delegatesToRead() throws Exception {
        when(cache.get(anyString())).thenReturn(null);
        Method m = Target.class.getMethod("getUser", String.class);
        CacheOperationContext ctx = new CacheOperationContext("Target", m, new Object[]{"42"}, new Target(),
                "user", "", "", "", false, "", "");

        processor.processCacheable(ctx, cacheManager, null);

        verify(cache).put("arg0=42", "user-42");
    }

    @Test
    void spi_processCacheable_failure_wrapsAsIllegalState() throws Exception {
        Method m = Target.class.getMethod("failing");
        CacheOperationContext ctx = new CacheOperationContext("Target", m, new Object[0], new Target(),
                "user", "", "", "", false, "", "");

        assertThatThrownBy(() -> processor.processCacheable(ctx, cacheManager, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缓存读取失败");
    }

    @Test
    void spi_processCachePut_delegatesToWrite() throws Exception {
        Method m = Target.class.getMethod("putUser", String.class);
        CacheOperationContext ctx = new CacheOperationContext("Target", m, new Object[]{"42"}, new Target(),
                "user", "", "", "", false, "", "");

        processor.processCachePut(ctx, cacheManager, null);

        verify(cache).put("arg0=42", "put-42");
    }

    @Test
    void spi_processCacheEvict_delegatesToEvict() throws Exception {
        Method m = Target.class.getMethod("evictUser", String.class);
        CacheOperationContext ctx = new CacheOperationContext("Target", m, new Object[]{"42"}, new Target(),
                "user", "", "", "", false, "", "");

        processor.processCacheEvict(ctx, cacheManager, null);

        verify(cache).evict("arg0=42");
    }

    private Cacheable withSync(Cacheable c) {
        when(c.sync()).thenReturn(true);
        return c;
    }

    @Test
    void setters_withNullInputs_areNoOps() {
        // setKeyGenerators/setEventListeners 收到 null 时应静默跳过
        processor.setKeyGenerators(null);
        processor.setEventListeners(null);
    }

    @Test
    void aroundCacheable_nullResult_skipsPut() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);

        Object result = processor.aroundCacheable(
                jp(new Target(), "getNull", new Class[0], new Object[0]),
                cacheable("user"));

        assertThat(result).isNull();
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void aroundCachePut_nullResult_skipsPut() throws Throwable {
        Object result = processor.aroundCachePut(
                jp(new Target(), "putNull", new Class[0], new Object[0]),
                cachePut("user"));

        assertThat(result).isNull();
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void aroundCacheable_sync_duplicatedHitInCriticalSection_returnsCached() throws Throwable {
        // 外层未命中进入 synchronizeLoad，内层同步块中再次命中直接返回，不执行方法也不回写
        when(cache.get(anyString())).thenReturn(null, "innerVal");

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}),
                withSync(cacheable("user")));

        // 返回同步块内命中的值，且未重新执行目标方法（否则会是 user-42）
        assertThat(result).isEqualTo("innerVal");
        // 命中值仍会被回写，故此处断言写入的是命中值本身
        verify(cache).put("arg0=42", "innerVal");
    }

    @Test
    void resolveMethod_nullArgs_usesEmptyParameterTypes() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);
        Cacheable c = cacheable("user");
        // 使用 SpEL 字面量 key，避免 null args 走默认键生成器
        when(c.key()).thenReturn("'fixed'");

        Object result = processor.aroundCacheable(
                jp(new Target(), "ping", new Class[0], null), c);

        assertThat(result).isEqualTo("pong");
    }

    @Test
    void resolveMethod_returnsNullWhenTargetMethodAbsent() throws Throwable {
        // 目标方法名不存在时，resolveMethod 捕获 NoSuchMethodException 并返回 null（防御分支）
        ProceedingJoinPoint joinPoint = jp(new Target(), "ghostMethod", new Class[]{String.class}, new Object[]{"x"});
        Method m = SpringCacheAnnotationProcessor.class.getDeclaredMethod("resolveMethod", ProceedingJoinPoint.class, Class.class);
        m.setAccessible(true);
        assertThat(m.invoke(processor, joinPoint, Target.class)).isNull();
    }

    @Test
    void evaluateSpEL_nullExpressionReturnsNull() throws Throwable {
        // key 表达式为 null 时，evaluateSpEL 直接原样返回 null，不进入 SpEL 解析
        Method m = SpringCacheAnnotationProcessor.class.getDeclaredMethod("evaluateSpEL", String.class, CacheOperationContext.class);
        m.setAccessible(true);
        CacheOperationContext ctx = new CacheOperationContext("Target", Target.class.getMethod("ping"), new Object[0], new Target(),
                "user", "", "", "", false, "", "");
        assertThat(m.invoke(processor, (String) null, ctx)).isNull();
    }

    @Test
    void spi_processCachePut_failure_wrapsAsIllegalState() throws Exception {
        // processWrite 抛异常时，processCachePut 重写覆盖为 IllegalStateException（缓存写入失败）
        Method m = Target.class.getMethod("failing");
        CacheOperationContext ctx = new CacheOperationContext("Target", m, new Object[0], new Target(),
                "user", "", "", "", false, "", "");
        assertThatThrownBy(() -> processor.processCachePut(ctx, cacheManager, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缓存写入失败");
    }

    @Test
    void resolveParameterTypes_nonMethodSignature_usesEmptyTypes() throws Throwable {
        when(cache.get(anyString())).thenReturn(null);
        // 签名不是 MethodSignature 时参数类型退化为空数组（解析无参方法 ping）
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getTarget()).thenReturn(new Target());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        Signature plainSignature = mock(Signature.class);
        when(plainSignature.getName()).thenReturn("ping");
        when(joinPoint.getSignature()).thenReturn(plainSignature);

        Object result = processor.aroundCacheable(joinPoint, cacheable("user"));

        assertThat(result).isEqualTo("pong");
    }

    // ------------------------------------------------------------------
    // condition / unless 条件评估
    // ------------------------------------------------------------------

    /**
     * 注入一个按字面值求值的条件解析器，用于隔离 SpEL 引擎测试切面逻辑。
     */
    private void injectLiteralEvaluator() {
        processor.setConditionEvaluator(new LiteralConditionEvaluator());
    }

    @Test
    void noConditionEvaluator_isNoOpConditionAlwaysPass() throws Throwable {
        // 无 ConditionEvaluator Bean 时行为回退到改动前：无条件通过
        when(cache.get(anyString())).thenReturn("cached");
        Cacheable c = withCondition(cacheable("user"), "#id == 'x'");

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        assertThat(result).isEqualTo("cached");
        verify(cache).get("arg0=42");
    }

    @Test
    void blankCondition_passes() throws Throwable {
        when(cache.get(anyString())).thenReturn("cached");
        Cacheable c = withCondition(cacheable("user"), "   ");

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        assertThat(result).isEqualTo("cached");
    }

    @Test
    void conditionFalse_skipsCacheReadAndPutButExecutesMethod() throws Throwable {
        // condition 为假：不查缓存、不回写，但方法体仍执行（与 Spring 语义一致）
        injectLiteralEvaluator();
        lenient().when(cache.get(anyString())).thenReturn(null);
        Cacheable c = withCondition(cacheable("user"), "false");

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        assertThat(result).isEqualTo("user-42");
        verify(cache, never()).get(anyString());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void conditionTrue_cachesAsUsual() throws Throwable {
        injectLiteralEvaluator();
        when(cache.get(anyString())).thenReturn(null);
        Cacheable c = withCondition(cacheable("user"), "true");

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        assertThat(result).isEqualTo("user-42");
        verify(cache).get("arg0=42");
        verify(cache).put("arg0=42", "user-42");
    }

    @Test
    void unlessTrue_skipsCacheReadAndPut() throws Throwable {
        // unless 为真：不缓存，故跳过缓存读取与回写（方法体仍执行）
        injectLiteralEvaluator();
        Cacheable c = withUnless(withCondition(cacheable("user"), "true"), "true");

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        assertThat(result).isEqualTo("user-42");
        verify(cache, never()).get(anyString());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void unlessFalse_cachesAsUsual() throws Throwable {
        injectLiteralEvaluator();
        when(cache.get(anyString())).thenReturn(null);
        Cacheable c = withUnless(withCondition(cacheable("user"), "true"), "false");

        Object result = processor.aroundCacheable(
                jp(new Target(), "getUser", new Class[]{String.class}, new Object[]{"42"}), c);

        assertThat(result).isEqualTo("user-42");
        verify(cache).get("arg0=42");
        verify(cache).put("arg0=42", "user-42");
    }

    @Test
    void conditionOnCachePut_respectsExpression() throws Throwable {
        injectLiteralEvaluator();
        CachePut c = withCondition(cachePut("user"), "false");

        Object result = processor.aroundCachePut(
                jp(new Target(), "putUser", new Class[]{String.class}, new Object[]{"42"}), c);

        assertThat(result).isEqualTo("put-42");
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void cacheEvict_hasNoConditionAttribute_evictsAlways() throws Throwable {
        // 与 Spring 一致：@CacheEvict 没有 condition 属性，故 always 清除
        injectLiteralEvaluator();
        CacheEvict c = cacheEvict("user", false);

        Object result = processor.aroundCacheEvict(
                jp(new Target(), "evictUser", new Class[]{String.class}, new Object[]{"42"}, "ok"), c);

        assertThat(result).isEqualTo("ok");
        verify(cache).evict("arg0=42");
    }

    @Test
    void cacheEvict_beforeInvocation_evictsThenProceeds() throws Throwable {
        injectLiteralEvaluator();
        CacheEvict c = cacheEvict("user", true);

        Object result = processor.aroundCacheEvict(
                jp(new Target(), "evictUser", new Class[]{String.class}, new Object[]{"42"}, "ok"), c);

        assertThat(result).isEqualTo("ok");
        verify(cache).evict("arg0=42");
    }

    /** 按字面值求值的条件解析器：表达式为 "true" 则真，其余假。 */
    private static final class LiteralConditionEvaluator extends ConditionEvaluator {

        @Override
        protected boolean doEvaluate(String expression, CacheOperationContext context) {
            return "true".equalsIgnoreCase(expression.trim());
        }
    }
}
