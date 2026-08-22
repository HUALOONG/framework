/**
 * framework-core：框架基础设施层（L0）。
 *
 * <p>提供 SPI 扩展机制、统一异常体系、断言工具、生命周期管理、事件机制与上下文传播，
 * 唯一外部依赖 JSpecify，零 Spring 依赖。本包统一启用空安全注解（{@code @NullMarked}）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
package cn.jowen.framework.core;

import org.jspecify.annotations.NullMarked;
