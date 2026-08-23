package cn.jowen.framework.extras.excel;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Excel 导入结果，包含成功数据与错误信息。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record ExcelImportResult<T>(
        /** 成功导入的数据列表。 */
        List<T> data,
        /** 错误记录列表。 */
        List<ExcelError> errors,
        /** 总行数（不含表头）。 */
        int totalCount,
        /** 成功行数。 */
        int successCount,
        /** 失败行数。 */
        int failCount
) {

    /**
     * 创建空结果。
     */
    public static <T> ExcelImportResult<T> empty() {
        return new ExcelImportResult<>(List.of(), List.of(), 0, 0, 0);
    }

    /**
     * 是否全部导入成功。
     */
    public boolean isSuccess() {
        return failCount == 0;
    }
}
