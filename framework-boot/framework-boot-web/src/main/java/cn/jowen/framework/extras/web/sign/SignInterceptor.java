package cn.jowen.framework.extras.web.sign;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Enumeration;

/**
 * {@link Sign} 注解的请求签名校验拦截器。
 *
 * <p>校验流程：
 * <ol>
 *   <li>从 {@link Sign#timestampHeader()} 读取时间戳，判断是否在 {@link Sign#toleranceSeconds()} 容忍窗口内；</li>
 *   <li>按 {@link Sign#fields()} 顺序拼接请求参数（或取原始 body）与密钥计算签名；</li>
 *   <li>与 {@link Sign#header()} 中的客户端签名做常量时间比较。</li>
 * </ol>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SignInterceptor implements HandlerInterceptor {

    /** verifier 不可变字段。 */
    private final SignVerifier verifier;

    /**
     * 构造实例。
     * @param verifier 参数 verifier
     */
    public SignInterceptor(SignVerifier verifier) {
        this.verifier = verifier;
    }

    /**
     * 执行pre handle操作。
     * @param request 参数 request
     * @param response 参数 response
     * @param handler 参数 handler
     * @return 结果
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        Sign sign = hm.getMethodAnnotation(Sign.class);
        if (sign == null) {
            sign = hm.getBeanType().getAnnotation(Sign.class);
        }
        if (sign == null) {
            return true;
        }

        long timestamp = parseHeader(request, sign.timestampHeader(), -1L);
        long now = System.currentTimeMillis();
        if (timestamp < 0 || Math.abs(now - timestamp) > sign.toleranceSeconds() * 1000L) {
            throw new ExtrasException("请求时间戳超出容忍范围");
        }

        String clientSig = request.getHeader(sign.header());
        if (clientSig == null || clientSig.isBlank()) {
            throw new ExtrasException(sign.message());
        }

        String appId = request.getHeader("X-App-Id");
        String payload = buildPayload(request, sign);
        if (!verifier.verify(appId == null ? "" : appId, payload, clientSig, timestamp)) {
            throw new ExtrasException(sign.message());
        }
        return true;
    }

    private long parseHeader(HttpServletRequest request, String name, long fallback) {
        String v = request.getHeader(name);
        if (v == null) {
            return fallback;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String buildPayload(HttpServletRequest request, Sign sign) {
        StringBuilder sb = new StringBuilder();
        if (sign.fields().length == 0) {
            Enumeration<String> names = request.getParameterNames();
            while (names.hasMoreElements()) {
                String n = names.nextElement();
                sb.append(n).append('=').append(request.getParameter(n)).append('&');
            }
        } else {
            for (String f : sign.fields()) {
                sb.append(f).append('=').append(request.getParameter(f)).append('&');
            }
        }
        return sb.toString();
    }
}
