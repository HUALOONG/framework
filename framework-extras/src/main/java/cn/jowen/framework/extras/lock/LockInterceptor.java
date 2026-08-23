package cn.jowen.framework.extras.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NullMarked;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 锁注解 AOP 拦截器。
 *
 * <p>环绕 {@link Lockable} 注解方法，在方法执行前获取锁，执行后释放。
 * 支持全部锁类型（REENTRANT / FAIR / READ / WRITE / MULTI / RED）。
 */
@NullMarked
@Aspect
@Component
public class LockInterceptor {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final DistributedLockManager _lockManager;

    public LockInterceptor(DistributedLockManager lockManager) {
        this._lockManager = lockManager;
    }

    @Around("@annotation(lockable)")
    public Object around(ProceedingJoinPoint pjp, Lockable lockable) throws Throwable {
        String lockName = resolveKey(lockable.key(), pjp);
        DistributedLock lock = selectLock(lockable.lockType(), lockName);

        boolean locked = lock.tryLock(lockable.waitTime(), TimeUnit.MILLISECONDS);
        if (!locked) {
            String message = lockable.failMessage().isEmpty()
                    ? "Failed to acquire lock: " + lockName
                    : lockable.failMessage();
            throw new LockException(message);
        }

        try {
            return pjp.proceed();
        } finally {
            lock.unlock();
        }
    }

    private DistributedLock selectLock(LockType lockType, String lockName) {
        return switch (lockType) {
            case FAIR -> _lockManager.getFairLock(lockName);
            case READ -> _lockManager.getReadLock(lockName);
            case WRITE -> _lockManager.getWriteLock(lockName);
            case MULTI -> _lockManager.getMultiLock(lockName);
            default -> _lockManager.getLock(lockName);
        };
    }

    private String resolveKey(String keyExpression, ProceedingJoinPoint pjp) {
        if (!keyExpression.startsWith("#")) {
            return keyExpression;
        }
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("args", pjp.getArgs());
        ctx.setVariable("target", pjp.getTarget());
        ctx.setVariable("methodName", method.getName());
        return PARSER.parseExpression(keyExpression).getValue(ctx, String.class);
    }
}