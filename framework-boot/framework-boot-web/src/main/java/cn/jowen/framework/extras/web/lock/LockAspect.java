package cn.jowen.framework.extras.web.lock;

import cn.jowen.framework.extras.web.util.SpelUtils;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * {@link Lockable} 注解的 AOP 织入切面。
 *
 * <p>基于 {@code key} 维度加锁，支持本地锁与（可选）分布式锁。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Aspect
@Order(200)
public class LockAspect {

    /** log 常量。 */
    private static final Logger log = LoggerFactory.getLogger(LockAspect.class);

    /** distributedLock 不可变字段。 */
    private final @Nullable DistributedLock distributedLock;

    /**
     * 构造实例。
     * @param distributedLock 参数 distributedLock
     */
    @Autowired
    public LockAspect(@Nullable DistributedLock distributedLock) {
        this.distributedLock = distributedLock;
    }

    /**
     * 执行@ around操作。
     * @param pjp 参数 pjp
     * @param lockable 参数 lockable
     * @return 结果
     * @throws Throwable Throwable 异常
     */
    @Around("@annotation(lockable)")
    public Object around(ProceedingJoinPoint pjp, Lockable lockable) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String key = "lock:" + SpelUtils.resolve(lockable.key(), pjp.getTarget(), method,
                pjp.getArgs(), pjp.getTarget().getClass().getName() + "." + method.getName());

        Lock lock = resolveLock(lockable, key);
        lock.lock();
        try {
            return pjp.proceed();
        } finally {
            lock.close();
        }
    }

    private Lock resolveLock(Lockable lockable, String key) {
        if (lockable.type() == LockType.REDIS) {
            if (distributedLock != null) {
                return distributedLock.acquire(key, lockable.waitMillis(), lockable.leaseMillis());
            }
            log.warn("未配置 DistributedLock 实现，REDIS 锁降级为本地锁: {}", key);
        }
        return new LocalLock(key, lockable.waitMillis());
    }
}
