package cn.jowen.framework.core.spi;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
 * @author Jowen
 * @date 2026-08-21
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
}
