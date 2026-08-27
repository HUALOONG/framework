package cn.jowen.framework.core.spi;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口为可扩展点（SPI）。被标注的接口可经 {@link ExtensionLoader} 加载实现。
 *
 * <p>示例：
 * <pre>{@code
 * @SPI
 * public interface Filter { void doFilter(); }
 * }</pre>
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {

    /**
     * 默认实现名（对应实现类上 {@link SPIImplementation#name()}）。为空表示无默认实现。
     *
     * @return 默认实现名
     */
    String value() default "";

    /**
     * 扩展点唯一标识，为空时默认使用接口全限定名。
     *
     * <p>用于与插件侧 {@code @Extension.extensionPoint} 对齐（见 framework-plugin 的
     * {@code PluginSpiBridge}），使扩展点可在不依赖接口全限定名的前提下被跨模块引用。
     *
     * @return 扩展点标识
     */
    String id() default "";
}
