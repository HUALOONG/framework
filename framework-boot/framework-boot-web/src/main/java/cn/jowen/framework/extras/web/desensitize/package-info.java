/**
 * Web 返回值脱敏适配：方法标注 {@code @Desensitized} 后，
 * 返回值（单对象 / List / 数组 / Map）按字段上的 {@code @DesensitizeField} 策略脱敏。
 *
 * <p>策略执行委托核心层 {@code cn.jowen.framework.core.desensitize.Desensitizer}，
 * 本包仅做 Web 形态的适配与 AOP 织入。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
package cn.jowen.framework.extras.web.desensitize;
