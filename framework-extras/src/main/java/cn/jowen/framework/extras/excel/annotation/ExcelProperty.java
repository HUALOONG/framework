package cn.jowen.framework.extras.excel.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 列映射注解，标记在字段上以指定列标题和转换器。
 *
 * <p>与 EasyExcel 的 {@code @ExcelProperty} 功能相似，但作为框架自定义注解，
 * 不依赖 EasyExcel 类路径，可在无 Excel 依赖时使用。
 *
 * @author 王飞
 * @since 2026-08-22
 * @see com.alibaba.excel.annotation.ExcelProperty
 */
@NullMarked
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelProperty {

    /**
     * 列标题（支持多行，数组元素为逐行标题）。
     */
    String[] value() default {};

    /**
     * 列索引（从 0 开始），与 {@link #value()} 二选一。
     */
    int index() default -1;

    /**
     * 转换器全限定类名，可为空（使用默认转换器）。
     */
    String converter() default "";

    /**
     * 格式化字符串（如日期格式 "yyyy-MM-dd"）。
     */
    String format() default "";
}
