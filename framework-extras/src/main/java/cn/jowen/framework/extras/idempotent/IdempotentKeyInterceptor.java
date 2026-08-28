package cn.jowen.framework.extras.idempotent;

import cn.jowen.framework.extras.config.IdempotentProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 幂等控制切面（KEY 模式）。
 *
 * <p>环绕 {@link IdempotentKey} 注解方法：按 SpEL 计算业务唯一键，经 {@link IdempotentValidator}
 * 执行 SETNX 占用——首次放行、重复抛 {@link IdempotencyException}；业务异常时回滚键允许重试。
 *
 * <p>SpEL 上下文支持 {@code #参数名}（经 {@link DefaultParameterNameDiscoverer} 绑定）、
 * {@code #a0/#p0} 索引、{@code #args}、{@code #target}、{@code #methodName}。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
@Aspect
@Component
public class IdempotentKeyInterceptor {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    private final IdempotentValidator validator;
    private final IdempotentProperties properties;

    /**
     * 构造切面。
     *
     * @param validator  幂等校验器，不可为 {@code null}
     * @param properties 幂等配置，不可为 {@code null}
     */
    public IdempotentKeyInterceptor(IdempotentValidator validator, IdempotentProperties properties) {
        if (validator == null) {
            throw new IllegalArgumentException("validator must not be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties must not be null");
        }
        this.validator = validator;
        this.properties = properties;
    }

    /**
     * 幂等拦截：占用键成功后放行，重复或异常回滚。
     *
     * @param pjp          连接点
     * @param idempotentKey 注解实例
     * @return 方法返回值
     * @throws Throwable 方法异常
     */
    @Around("@annotation(idempotentKey)")
    public Object around(ProceedingJoinPoint pjp, IdempotentKey idempotentKey) throws Throwable {
        String key = properties.getKeyPrefix() + resolveKey(idempotentKey.key(), pjp);
        if (!validator.validate(key)) {
            // 键已被占用：判为重复提交
            throw new IdempotencyException(idempotentKey.message());
        }
        try {
            return pjp.proceed();
        } catch (RuntimeException | Error e) {
            // 业务异常回滚键，允许客户端重试
            validator.remove(key);
            throw e;
        }
    }

    private String resolveKey(String expression, ProceedingJoinPoint pjp) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("idempotent key 不能为空");
        }
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Object[] args = pjp.getArgs() == null ? new Object[0] : pjp.getArgs();
        ctx.setVariable("args", args);
        ctx.setVariable("target", pjp.getTarget());
        ctx.setVariable("methodName", method.getName());
        String[] names = PARAMETER_NAMES.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            ctx.setVariable("a" + i, args[i]);
            ctx.setVariable("p" + i, args[i]);
            if (names != null && i < names.length && names[i] != null) {
                ctx.setVariable(names[i], args[i]);
            }
        }
        Object value = PARSER.parseExpression(expression).getValue(ctx, String.class);
        if (value == null) {
            throw new IllegalArgumentException("幂等 key 求值为空: " + expression);
        }
        return value.toString();
    }
}