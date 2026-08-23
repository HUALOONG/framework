package cn.jowen.framework.extras.excel;

import org.jspecify.annotations.NullMarked;

/**
 * Excel 行校验接口，导入时对每一行数据进行校验。
 *
 * @param <T> 数据行类型
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
@FunctionalInterface
public interface ExcelRowValidator<T> {

    /**
     * 校验单行数据，返回 true 表示校验通过。
     *
     * @param row      数据行对象
     * @param rowIndex 行号（从 0 开始，不含表头）
     * @return true 表示通过，false 表示失败
     */
    boolean validate(T row, int rowIndex);
}
