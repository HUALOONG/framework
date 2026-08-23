package cn.jowen.framework.extras.operatelog;

import org.jspecify.annotations.NullMarked;

/**
 * 操作日志拦截器（骨架）。
 *
 * <p>前置记录开始时间，后置解析 SpEL 组装记录，异常记录错误，最终异步写入虚拟线程池。
 * 当前由 {@link OperateLogAspect} 承担 AOP 切面职责，本类保留作为 SPI 扩展点。
 *
 * @author 王飞
 * @since 2026-08-25
 * @see OperateLogAspect
 */
@NullMarked
public final class OperateLogInterceptor {

    // TODO: 实现操作日志拦截器逻辑（当前由 OperateLogAspect 实现）
}
