package cn.jowen.framework.extras.web.operatelog;

import org.jspecify.annotations.NullMarked;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * {@link OperateLog} 注解的 AOP 织入切面：记录操作人、耗时、成功与失败信息。
 *
 * <p>异步记录由 {@link Executor} 承载；未配置时使用调用线程同步执行。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Aspect
public final class OperateLogAspect {

    /** handler 不可变字段。 */
    private final OperateLogHandler handler;
    /** executor 不可变字段。 */
    private final Executor executor;
    /** async 不可变字段。 */
    private final boolean async;
    /** operatorProvider 不可变字段。 */
    private final OperatorProvider operatorProvider;

    /**
     * 构造实例。
     * @param handler 参数 handler
     * @param executor 参数 executor
     * @param async 参数 async
     * @param operatorProvider 参数 operatorProvider
     */
    public OperateLogAspect(OperateLogHandler handler, Executor executor, boolean async,
                            OperatorProvider operatorProvider) {
        this.handler = handler;
        this.executor = executor;
        this.async = async;
        this.operatorProvider = operatorProvider;
    }

    /**
     * 执行@ around操作。
     * @param pjp 参数 pjp
     * @param operateLog 参数 operateLog
     * @return 结果
     * @throws Throwable Throwable 异常
     */
    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint pjp, OperateLog operateLog) throws Throwable {
        long start = System.currentTimeMillis();
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String signature = pjp.getTarget().getClass().getName() + "#" + method.getName();

        Object result = null;
        Throwable failure = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            OperateLogEvent event = new OperateLogEvent(
                    operatorProvider.currentOperator(),
                    operateLog.module(),
                    operateLog.value().isEmpty() ? method.getName() : operateLog.value(),
                    signature,
                    operateLog.recordParams() ? Arrays.toString(pjp.getArgs()) : null,
                    operateLog.recordResult() && result != null ? String.valueOf(result) : null,
                    failure == null,
                    failure == null ? null : String.valueOf(failure.getMessage()),
                    cost,
                    Instant.now());
            if (async) {
                executor.execute(() -> handler.handle(event));
            } else {
                handler.handle(event);
            }
        }
    }

}
