package cn.jowen.framework.extras.idempotent;

import org.jspecify.annotations.NullMarked;

/**
 * 幂等控制拦截器。
 *
 * <p>解析 {@link Idempotent} 注解，执行幂等校验：
 * <ul>
 *   <li>TOKEN 模式：从请求头获取 token，校验后删除</li>
 *   <li>KEY 模式：使用 SpEL 计算唯一键，SETNX 原子操作</li>
 * </ul>
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class IdempotentInterceptor {

    // TODO: 实现幂等拦截器逻辑
}
