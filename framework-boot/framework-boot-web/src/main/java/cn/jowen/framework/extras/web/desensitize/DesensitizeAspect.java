package cn.jowen.framework.extras.web.desensitize;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.NullMarked;

/**
 * {@link Desensitized} 注解的 AOP 织入切面：
 * 方法返回值经 {@link DesensitizeSupport#mask(Object)} 脱敏后返回，
 * 字段级策略由 {@code @DesensitizeField} 声明。
 *
 * <p>无配置状态：脱敏策略注册在核心层 {@code Desensitizer} 单例中，
 * 本切面无需注入任何依赖。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Aspect
public class DesensitizeAspect {

    /**
     * 创建切面。
     */
    public DesensitizeAspect() {
    }

    /**
     * 环绕织入：执行目标方法并对返回值脱敏。
     *
     * @param pjp        连接点
     * @param desensitized 方法上的注解（仅用于切点绑定）
     * @return 脱敏后的返回值
     * @throws Throwable 目标方法抛出的异常原样上抛
     */
    @Around("@annotation(desensitized)")
    public Object around(ProceedingJoinPoint pjp, Desensitized desensitized) throws Throwable {
        Object result = pjp.proceed();
        return DesensitizeSupport.mask(result);
    }
}
