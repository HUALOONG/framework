package cn.jowen.framework.extras.idempotent;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;

/**
 * 幂等控制过滤器（TOKEN 模式）。
 *
 * <p>extras 无 spring-webmvc 依赖，故以 {@link jakarta.servlet.Filter} 实现而非 MVC 拦截器。
 * 当请求携带幂等令牌头时：首次放行并占用令牌，重复请求返回 409 拒绝；请求异常时回滚令牌允许重试。
 *
 * <p>KEY 模式需方法上下文（SpEL 计算唯一键），Filter 层不可得，由 {@link Idempotent} 注解 + AOP 承担。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class IdempotentInterceptor implements Filter {

    private final IdempotentValidator validator;
    private final String tokenHeader;

    /**
     * 构造过滤器。
     *
     * @param validator   幂等校验器，不可为 {@code null}
     * @param tokenHeader 幂等令牌请求头名，不可为空
     */
    public IdempotentInterceptor(IdempotentValidator validator, String tokenHeader) {
        if (validator == null) {
            throw new IllegalArgumentException("validator must not be null");
        }
        if (tokenHeader == null || tokenHeader.isBlank()) {
            throw new IllegalArgumentException("tokenHeader must not be blank");
        }
        this.validator = validator;
        this.tokenHeader = tokenHeader;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest http)) {
            chain.doFilter(request, response);
            return;
        }
        String token = http.getHeader(tokenHeader);
        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!validator.validate(token)) {
            // 令牌已被占用：判为重复提交
            if (response instanceof HttpServletResponse httpResponse) {
                httpResponse.setStatus(HttpServletResponse.SC_CONFLICT);
            }
            return;
        }
        try {
            chain.doFilter(request, response);
        } catch (RuntimeException | Error e) {
            // 业务异常回滚令牌，允许客户端重试
            validator.remove(token);
            throw e;
        }
    }
}