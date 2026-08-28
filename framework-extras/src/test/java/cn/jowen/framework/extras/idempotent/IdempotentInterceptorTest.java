package cn.jowen.framework.extras.idempotent;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link IdempotentInterceptor} 测试：无令牌放行、首次占用放行、重复 409 拒绝、异常回滚令牌。
 */
class IdempotentInterceptorTest {

    private static final String HEADER = "Idempotency-Key";

    private final LocalIdempotentValidator validator = new LocalIdempotentValidator(60_000L);

    @Test
    void noTokenHeader_passesThrough() throws Exception {
        IdempotentInterceptor filter = new IdempotentInterceptor(validator, HEADER);
        AtomicInteger chainCalls = new AtomicInteger();
        filter.doFilter(request(null), response(null), chain(chainCalls, null));
        assertThat(chainCalls.get()).isEqualTo(1);
    }

    @Test
    void firstRequest_occupiesToken_passes() throws Exception {
        IdempotentInterceptor filter = new IdempotentInterceptor(validator, HEADER);
        AtomicInteger chainCalls = new AtomicInteger();
        filter.doFilter(request("tok-1"), response(null), chain(chainCalls, null));
        assertThat(chainCalls.get()).isEqualTo(1);
    }

    @Test
    void duplicateRequest_returnsConflictAndSkipsChain() throws Exception {
        IdempotentInterceptor filter = new IdempotentInterceptor(validator, HEADER);
        AtomicReference<Integer> status = new AtomicReference<>();
        filter.doFilter(request("tok-1"), response(status), chain(new AtomicInteger(), null));
        // 首次已占用；同令牌再请求应 409 且不再进入业务
        filter.doFilter(request("tok-1"), response(status), chain(new AtomicInteger(), null));
        assertThat(status.get()).isEqualTo(HttpServletResponse.SC_CONFLICT);
    }

    @Test
    void chainException_rollsBackToken_allowsRetry() throws Exception {
        IdempotentInterceptor filter = new IdempotentInterceptor(validator, HEADER);
        // 首次请求业务抛异常 → 令牌回滚
        assertThatThrownBy(() -> filter.doFilter(request("tok-1"), response(null),
                chain(new AtomicInteger(), new RuntimeException("boom"))))
                .isInstanceOf(RuntimeException.class);
        // 重试应放行（令牌已回滚）
        AtomicInteger chainCalls = new AtomicInteger();
        filter.doFilter(request("tok-1"), response(null), chain(chainCalls, null));
        assertThat(chainCalls.get()).isEqualTo(1);
    }

    @Test
    void nullArgs_throw() {
        assertThatThrownBy(() -> new IdempotentInterceptor(null, HEADER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdempotentInterceptor(validator, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static HttpServletRequest request(String token) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                IdempotentInterceptorTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> method.getName().equals("getHeader") ? token : null);
    }

    private static HttpServletResponse response(AtomicReference<Integer> status) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                IdempotentInterceptorTest.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setStatus") && status != null) {
                        status.set((Integer) args[0]);
                    }
                    return null;
                });
    }

    private static FilterChain chain(AtomicInteger calls, RuntimeException failure) {
        return new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                calls.incrementAndGet();
                if (failure != null) {
                    throw failure;
                }
            }
        };
    }
}