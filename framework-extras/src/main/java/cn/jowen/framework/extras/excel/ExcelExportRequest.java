package cn.jowen.framework.extras.excel;

import cn.jowen.framework.extras.excel.handler.ExcelWriteHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Excel 导出请求参数。
 *
 * @param <T> 数据行类型
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record ExcelExportRequest<T>(
        /** 输出文件名（不含扩展名）。 */
        String fileName,
        /** sheet 名称。 */
        String sheetName,
        /** 数据列表。 */
        List<T> data,
        /** 表头 Class。 */
        Class<T> head,
        /** 自定义写入处理器，可为 null。 */
        @Nullable ExcelWriteHandler writeHandler
) {

    /**
     * 创建单 sheet 导出请求。
     *
     * @param fileName  文件名
     * @param sheetName sheet 名
     * @param data      数据
     * @param head      表头类型
     */
    public ExcelExportRequest(String fileName, String sheetName, List<T> data, Class<T> head) {
        this(fileName, sheetName, data, head, null);
    }
}
