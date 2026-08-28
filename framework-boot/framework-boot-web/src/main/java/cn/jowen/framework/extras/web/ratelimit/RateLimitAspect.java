package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.web.util.SpelUtils;
import org.jspecify.annotations.NullMarked;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * {@link RateLimit} 注解的 AOP 织入切面，基于令牌桶算法限流。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Aspect
@Order(100)
public class RateLimitAspect {

    /** manager 不可变字段。 */
    private final RateLimiterManager manager;

    /**
     * 构造实例。
     * @param manager 参数 manager
     */
    @Autowired
    public RateLimitAspect(RateLimiterManager manager) {
        this.manager = manager;
    }

    /**
     * 执行@ around操作。
     * @param pjp 参数 pjp
     * @param rateLimit 参数 rateLimit
     * @return 结果
     * @throws Throwable Throwable 异常
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = SpelUtils.resolve(rateLimit.key(), pjp.getTarget(), method, pjp.getArgs(),
                pjp.getTarget().getClass().getName() + "." + method.getName());

        RateLimiter limiter = manager.get(key, rateLimit.permits(), rateLimit.window());
        if (!limiter.tryAcquire(key)) {
            throw new ExtrasException(rateLimit.message());
        }
        return pjp.proceed();
    }
}
