package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * Excel 处理配置属性载体。
 *
 * <p>通过 {@code framework.extras.excel.*} 前缀绑定。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class ExcelProperties {

    /**
     * 是否启用 Excel 处理（默认 true）。
     */
    private boolean enabled = true;

    /**
     * 默认文件名（导出时使用，默认 report）。
     */
    private String defaultFileName = "report";

    /**
     * 默认 sheet 名称（默认 Sheet1）。
     */
    private String defaultSheetName = "Sheet1";

    /**
     * 导入批次大小（默认 500）。
     */
    private int importBatchSize = 500;

    /**
     * 导入时跳过的表头行数（默认 1）。
     */
    private int headerRowCount = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultFileName() {
        return defaultFileName;
    }

    public void setDefaultFileName(String defaultFileName) {
        this.defaultFileName = defaultFileName;
    }

    public String getDefaultSheetName() {
        return defaultSheetName;
    }

    public void setDefaultSheetName(String defaultSheetName) {
        this.defaultSheetName = defaultSheetName;
    }

    public int getImportBatchSize() {
        return importBatchSize;
    }

    public void setImportBatchSize(int importBatchSize) {
        this.importBatchSize = Math.max(1, importBatchSize);
    }

    public int getHeaderRowCount() {
        return headerRowCount;
    }

    public void setHeaderRowCount(int headerRowCount) {
        this.headerRowCount = Math.max(0, headerRowCount);
    }
}
