package cn.jowen.framework.extras.operatelog.aop;

import cn.jowen.framework.extras.datapermission.DataPermissionContext;
import cn.jowen.framework.extras.datapermission.UserInfo;
import cn.jowen.framework.extras.operatelog.OperateLog;
import cn.jowen.framework.extras.operatelog.OperateLogDispatcher;
import cn.jowen.framework.extras.operatelog.OperateLogRecord;
import cn.jowen.framework.extras.operatelog.OperateStatus;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * 操作日志 AOP 切面。
 *
 * <p>环绕 {@link OperateLog} 注解方法，收集参数/返回值/耗时/异常，组装 {@link OperateLogRecord}
 * 后通过 {@link OperateLogDispatcher} 分发（{@code async=true} 异步 / {@code async=false} 同步）。
 *
 * <p>依赖 {@code spring-aop} + {@code aspectjweaver}（optional），未引入时不注册。
 *
 * @author 王飞
 * @since 2026-08-22
 * @see OperateLog
 * @see OperateLogDispatcher
 */
@NullMarked
@Aspect
@Component
@ConditionalOnClass(name = {
        "cn.jowen.framework.extras.operatelog.OperateLog",
        "cn.jowen.framework.extras.operatelog.OperateLogDispatcher"
})
@ConditionalOnBean(OperateLogDispatcher.class)
public class OperateLogAspect {

    private final OperateLogDispatcher dispatcher;

    public OperateLogAspect(OperateLogDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
    }

    @Around("@annotation(oplog)")
    public Object around(ProceedingJoinPoint pjp, OperateLog oplog) throws Throwable {
        long start = System.currentTimeMillis();
        OperateStatus status = null;
        String errorMsg = null;
        Object result = null;
        try {
            result = pjp.proceed();
            status = OperateStatus.SUCCESS;
        } catch (Throwable t) {
            status = OperateStatus.FAIL;
            errorMsg = t.getMessage();
            throw t;
        } finally {
            long costTime = System.currentTimeMillis() - start;
            OperateLogRecord record = buildRecord(pjp, oplog, result, status, errorMsg, costTime);
            if (oplog.async()) {
                dispatcher.dispatch(record);
            } else {
                dispatcher.dispatchSync(record);
            }
        }
        return result;
    }

    private OperateLogRecord buildRecord(ProceedingJoinPoint pjp, OperateLog oplog,
                                         Object result, OperateStatus status,
                                         String errorMsg, long costTime) {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = ms.getName();

        String content;
        if (oplog.content().isEmpty()) {
            Object[] args = pjp.getArgs();
            content = args != null ? Arrays.toString(args) : "";
        } else {
            content = oplog.content();
        }

        // operator 从 DataPermissionContext 获取（由 DataPermissionAspect 注入）
        String operator = null;
        String operatorId = null;
        UserInfo currentUser = DataPermissionContext.getCurrentUser();
        if (currentUser != null) {
            operator = currentUser.userId() != null ? String.valueOf(currentUser.userId()) : null;
            operatorId = operator;
        }

        return OperateLogRecord.builder()
                .module(oplog.module())
                .action(oplog.action())
                .description(oplog.description())
                .content(content)
                .operator(operator)
                .operatorId(operatorId)
                .status(status)
                .errorMessage(errorMsg)
                .costTime(costTime)
                .extra("method", className + "." + methodName)
                .extra("className", className)
                .build();
    }
}
