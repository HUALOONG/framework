package cn.jowen.framework.extras.web.datapermission;

import cn.jowen.framework.extras.properties.DataScope;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * {@link DataPermission} 注解的 AOP 织入切面：
 * 方法执行期间将声明的数据范围写入 {@link DataPermissionContext}，结束后清理。
 *
 * <p>具体的 SQL 条件注入由持久层拦截器结合 {@link DataPermissionRule} 读取上下文完成。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Aspect
public final class DataPermissionAspect {

    /** rule 不可变字段。 */
    private final DataPermissionRule rule;
    /** defaultScope 不可变字段。 */
    private final DataScope defaultScope;

    /**
     * 创建切面，使用 {@link DataScope#ALL} 作为默认数据范围（等价于不追加默认过滤）。
     *
     * @param rule 数据权限规则
     */
    public DataPermissionAspect(DataPermissionRule rule) {
        this(rule, DataScope.ALL);
    }

    /**
     * 创建切面并指定默认数据范围。
     *
     * @param rule         数据权限规则
     * @param defaultScope 方法<b>未标注</b> {@link DataPermission} 时使用的数据范围
     */
    public DataPermissionAspect(DataPermissionRule rule, DataScope defaultScope) {
        this.rule = rule;
        this.defaultScope = defaultScope;
    }

    /**
     * 执行@ around操作。
     * @param pjp 参数 pjp
     * @param dataPermission 参数 dataPermission
     * @return 结果
     * @throws Throwable Throwable 异常
     */
    @Around("@annotation(dataPermission)")
    public Object around(ProceedingJoinPoint pjp, DataPermission dataPermission) throws Throwable {
        DataPermissionContext.set(dataPermission.scope(), dataPermission.table());
        try {
            return pjp.proceed();
        } finally {
            DataPermissionContext.clear();
        }
    }

    /**
     * @return 当前上下文对应的过滤条件（供持久层调用）
     *
     * <p>取值优先级：
     * <ol>
     *   <li>方法标注了 {@link DataPermission} —— 使用该注解声明的数据范围；</li>
     *   <li>方法未标注（无活跃上下文）—— 使用配置的 {@code defaultScope}。</li>
     * </ol>
     *
     * <p>{@code defaultScope} 默认为 {@link DataScope#ALL}，即未标注的方法不追加任何过滤，
     * 与未配置时行为一致；仅当显式配置为受限范围时，未标注方法才被自动过滤。
     * 这样既让配置真正生效，又不会覆盖显式标注 {@code scope = ALL} 的放行语义。
     */
    public @Nullable String currentCondition() {
        DataScope scope = DataPermissionContext.isActive()
                ? DataPermissionContext.currentScope()
                : defaultScope;
        return rule.condition(scope, DataPermissionContext.currentTable());
    }

    /** @return 当前目标方法（诊断用） */
    static Method targetMethod(ProceedingJoinPoint pjp) {
        return ((MethodSignature) pjp.getSignature()).getMethod();
    }
}
