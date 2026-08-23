package cn.jowen.framework.extras.excel.handler;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import org.jspecify.annotations.NullMarked;

/**
 * Excel 写入处理器接口，封装 EasyExcel 的 {@link SheetWriteHandler}。
 *
 * <p>用户可实现此接口自定义列宽、合并单元格、样式等写入行为。
 *
 * @author 王飞
 * @since 2026-08-22
 * @see com.alibaba.excel.write.handler.SheetWriteHandler
 */
@NullMarked
public interface ExcelWriteHandler extends SheetWriteHandler {

    /**
     * 在 sheet 创建后、写入数据前回调。
     *
     * @param sheetHolder sheet 写入上下文
     * @param table       表信息
     */
    default void afterSheetCreate(WriteSheetHolder sheetHolder, WriteTableHolder table) {
        // 默认空实现
    }

    /**
     * 在 sheet 所有数据写入完成后回调。
     *
     * @param sheetHolder sheet 写入上下文
     * @param table       表信息
     */
    default void afterSheetDispose(WriteSheetHolder sheetHolder, WriteTableHolder table) {
        // 默认空实现
    }
}
