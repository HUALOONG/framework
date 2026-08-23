package cn.jowen.framework.extras.excel.handler;

import org.jspecify.annotations.NullMarked;

/**
 * Excel 读取监听器接口，封装 EasyExcel 的 {@code ReadListener}。
 *
 * <p>用户可实现此接口自定义每行数据的处理逻辑。
 *
 * @param <T> 数据行类型
 * @author 王飞
 * @since 2026-08-22
 * @see com.alibaba.excel.read.listener.ReadListener
 */
@NullMarked
public interface ExcelReadListener<T> {

    /**
     * 解析表头时回调。
     *
     * @param head     表头行数据（List&lt;String&gt;）
     * @param rowIndex 表头行号
     */
    default void onHead(Object head, int rowIndex) {
        // 默认空实现
    }

    /**
     * 解析每行数据时回调。
     *
     * @param row      数据行对象
     * @param rowIndex 行号（从 0 开始，不含表头）
     */
    void onData(T row, int rowIndex);

    /**
     * 全部数据解析完成时回调。
     */
    default void onComplete() {
        // 默认空实现
    }
}
