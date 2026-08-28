package cn.jowen.framework.extras.web.idempotent;

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
import java.util.concurrent.TimeUnit;

/**
 * {@link Idempotent} 注解的 AOP 织入切面，基于指纹存储防重复提交。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Aspect
@Order(150)
public class IdempotentAspect {

    /** store 不可变字段。 */
    private final IdempotentStore store;

    /**
     * 构造实例。
     * @param store 参数 store
     */
    @Autowired
    public IdempotentAspect(IdempotentStore store) {
        this.store = store;
    }

    /**
     * 执行@ around操作。
     * @param pjp 参数 pjp
     * @param idempotent 参数 idempotent
     * @return 结果
     * @throws Throwable Throwable 异常
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = "idem:" + SpelUtils.resolve(idempotent.key(), pjp.getTarget(), method,
                pjp.getArgs(), pjp.getTarget().getClass().getName() + "." + method.getName());

        if (!store.tryMark(key, idempotent.expire(), TimeUnit.SECONDS)) {
            throw new ExtrasException(idempotent.message());
        }
        try {
            return pjp.proceed();
        } finally {
            store.remove(key);
        }
    }
}
