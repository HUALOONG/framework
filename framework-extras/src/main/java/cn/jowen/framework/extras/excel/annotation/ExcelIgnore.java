package cn.jowen.framework.extras.excel.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 忽略字段注解，标记在不需要导出/导入的字段上。
 *
 * <p>与 EasyExcel 的 {@code @ExcelIgnore} 功能相似，但作为框架自定义注解，
 * 不依赖 EasyExcel 类路径，可在无 Excel 依赖时使用。
 *
 * @author 王飞
 * @since 2026-08-22
 * @see com.alibaba.excel.annotation.ExcelIgnore
 */
@NullMarked
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelIgnore {
}
