/**
 * framework-logger：统一日志门面模块，提供脱敏、链路追踪增强、异步与结构化日志能力。
 *
 * <p>本包统一启用空安全注解（{@code @NullMarked}），业务代码仅依赖 facade 子包，不直接感知底层实现。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
package cn.jowen.framework.logger;

import org.jspecify.annotations.NullMarked;
