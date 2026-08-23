package cn.jowen.framework.extras.excel.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 合并单元格注解，标记在字段上以指定按该字段值合并相邻行的单元格。
 *
 * <p>使用方式：在导出时 EasyExcel 会根据该字段相同值自动合并单元格。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelMerge {

    /**
     * 合并方向：COLUMN（纵向合并）或 ROW（横向合并），默认 COLUMN。
     */
    MergeDirection direction() default MergeDirection.COLUMN;

    enum MergeDirection {
        COLUMN,
        ROW
    }
}
