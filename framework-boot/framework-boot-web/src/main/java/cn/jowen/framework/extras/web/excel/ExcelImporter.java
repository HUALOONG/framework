package cn.jowen.framework.extras.web.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import org.jspecify.annotations.NullMarked;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Excel 导入器：基于 EasyExcel 将 xlsx 流读取为 {@code List<T>}。
 *
 * <p>行数上限保护：导入过程中累计行数一旦超过 {@code maxRows} 立即中断解析，
 * 避免恶意/误操作的大文件把堆内存耗尽——不能等 {@code doRead()} 结束后再检查，
 * 那样全量数据早已驻留内存。
 *
 * <p>本类不依赖 Spring，可脱离容器独立使用。
 *
 * <pre>{@code
 * ExcelImporter importer = new ExcelImporter(100_000);
 * List<UserRow> users = importer.read(inputStream, UserRow.class);
 * }</pre>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ExcelImporter {

    /** 单次导入最大行数。 */
    private final int maxRows;

    /**
     * 构造实例。
     *
     * @param maxRows 单次导入最大行数，必须为正数
     * @throws IllegalArgumentException maxRows 非正数时抛出
     */
    public ExcelImporter(int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be positive: " + maxRows);
        }
        this.maxRows = maxRows;
    }

    /**
     * 从输入流读取全部数据行。
     *
     * @param in   源流（由调用方负责关闭）
     * @param type 行模型类型（含 EasyExcel 列注解）
     * @param <T>  行模型类型
     * @return 数据行列表，无数据时为空列表
     * @throws ExcelException 行数超限或底层解析失败
     */
    public <T> List<T> read(InputStream in, Class<T> type) {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(type, "type");

        List<T> result = new ArrayList<>();
        AtomicBoolean overflow = new AtomicBoolean(false);
        AnalysisEventListener<T> listener = new AnalysisEventListener<>() {
            @Override
            public void invoke(T row, AnalysisContext context) {
                if (result.size() >= maxRows) {
                    overflow.set(true);
                    // 抛出以中断解析；EasyExcel 会包装异常，外层依据 overflow 标记还原为 ExcelException
                    throw new IllegalStateException("导入行数超过上限 " + maxRows);
                }
                result.add(row);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 无收尾动作
            }
        };
        try {
            EasyExcel.read(in, type, listener).sheet().doRead();
        } catch (RuntimeException e) {
            if (overflow.get()) {
                throw new ExcelException(
                        "导入行数超过上限 " + maxRows + "，已中断解析，请分批导入", e);
            }
            throw new ExcelException("Excel 导入失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 单次导入最大行数。
     *
     * @return 行数上限
     */
    public int getMaxRows() {
        return maxRows;
    }
}
