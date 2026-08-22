/**
 * SPI 扩展机制，提供类 Dubbo 风格的扩展点加载、@SPI 声明与 @Activate 自动激活。
 *
 * <p>基于 JDK {@link java.util.ServiceLoader} 之上封装，支持默认实现、按名获取与排序激活。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
package cn.jowen.framework.core.spi;

import org.jspecify.annotations.NullMarked;
