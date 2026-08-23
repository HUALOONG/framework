package cn.jowen.framework.extras.ratelimit;

import cn.jowen.framework.extras.exception.RateLimitException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流拦截器。
 *
 * <p>拦截流程：
 * <ol>
 *   <li>解析 {@link RateLimit} 注解，提取 key、algorithm、permits、period、scope、message</li>
 *   <li>按 scope 追加维度后缀（GLOBAL 无后缀；USER/IP/CUSTOM 拼接维度值）</li>
 *   <li>SpEL 求值（支持 {@code #root} / {@code #args} 上下文）</li>
 *   <li>从 {@link RateLimiterManager} 取限流器并 {@code tryAcquire()}</li>
 *   <li>限流则抛 {@link RateLimitException}，否则放行</li>
 * </ol>
 *
 * <p>SpEL 表达式缓存于 {@link ConcurrentHashMap}，避免重复解析。
 */
@NullMarked
public final class RateLimitInterceptor {

    private final RateLimiterManager _manager;
    private final ExpressionParser _parser = new SpelExpressionParser();
    private final Map<String, org.springframework.expression.Expression> _keyCache = new ConcurrentHashMap<>();

    public RateLimitInterceptor(RateLimiterManager manager) {
        this._manager = manager;
    }

    /**
     * 拦截限流，返回是否放行。
     *
     * @param rateLimit 注解实例
     * @param scope     作用域，可为 {@code null}（等价于 GLOBAL）
     * @param scopeValue 作用域值（USER 时为用户 ID，IP 时为客户端 IP 等）
     * @return {@code true} 放行，{@code false} 限流
     * @throws RateLimitException 限流时抛出
     */
    public boolean intercept(RateLimit rateLimit, @Nullable RateLimitScope scope, @Nullable String scopeValue)
            throws RateLimitException {
        String key = resolveKey(rateLimit, scope, scopeValue);
        RateLimiter limiter = _manager.getLimiter(
                key, rateLimit.algorithm(), rateLimit.permits(), rateLimit.period());
        if (limiter.tryAcquire()) {
            return true;
        }
        throw new RateLimitException(rateLimit.message());
    }

    /**
     * 拦截限流，返回是否放行；不抛异常。
     *
     * @param rateLimit 注解实例
     * @param scope     作用域，可为 {@code null}
     * @param scopeValue 作用域值
     * @return {@code true} 放行，{@code false} 限流
     */
    public boolean tryIntercept(RateLimit rateLimit, @Nullable RateLimitScope scope, @Nullable String scopeValue) {
        try {
            return intercept(rateLimit, scope, scopeValue);
        } catch (RateLimitException e) {
            return false;
        }
    }

    private String resolveKey(RateLimit rateLimit, @Nullable RateLimitScope scope, @Nullable String scopeValue) {
        String raw = rateLimit.key();
        String suffix = resolveScopeSuffix(scope, scopeValue);
        if (suffix != null) {
            raw = raw + ":" + suffix;
        }
        try {
            org.springframework.expression.Expression expr = _keyCache.computeIfAbsent(raw, _parser::parseExpression);
            EvaluationContext ctx = new StandardEvaluationContext();
            ctx.setVariable("root", Map.of());
            ctx.setVariable("args", Map.of());
            Object value = expr.getValue(ctx);
            return value == null ? raw : value.toString();
        } catch (Exception e) {
            return raw;
        }
    }

    private @Nullable String resolveScopeSuffix(@Nullable RateLimitScope scope, @Nullable String scopeValue) {
        if (scope == null || scope == RateLimitScope.GLOBAL) {
            return null;
        }
        if (scope == RateLimitScope.CUSTOM) {
            return scopeValue != null ? scopeValue : "custom";
        }
        return scope.name().toLowerCase() + ":" + (scopeValue != null ? scopeValue : "unknown");
    }
}