package cn.jowen.framework.extras.web.excel;

import com.alibaba.excel.EasyExcel;
import org.jspecify.annotations.NullMarked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

/**
 * Excel 导出器：基于 EasyExcel 将 {@code List<T>} 写出为 xlsx 字节流。
 *
 * <p>行数上限保护：单次导出超过 {@code maxRows} 时直接拒绝，
 * 防止大结果集一次性打爆内存（EasyExcel 写侧本身是流式的，
 * 但业务数据 {@code List} 已经全量驻留在堆内，必须前置拦截）。
 *
 * <p>本类不依赖 Spring，可脱离容器独立使用。
 *
 * <pre>{@code
 * ExcelExporter exporter = new ExcelExporter(100_000);
 * byte[] bytes = exporter.export("用户数据", users, UserRow.class);
 * }</pre>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ExcelExporter {

    /** 单次导出最大行数。 */
    private final int maxRows;

    /**
     * 构造实例。
     *
     * @param maxRows 单次导出最大行数，必须为正数
     * @throws IllegalArgumentException maxRows 非正数时抛出
     */
    public ExcelExporter(int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be positive: " + maxRows);
        }
        this.maxRows = maxRows;
    }

    /**
     * 导出为字节数组。
     *
     * @param sheetName sheet 名称
     * @param rows      数据行
     * @param type      行模型类型（含 EasyExcel 列注解）
     * @param <T>       行模型类型
     * @return xlsx 字节数组
     * @throws ExcelException 行数超限或底层写出失败
     */
    public <T> byte[] export(String sheetName, List<T> rows, Class<T> type) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, sheetName, rows, type);
        return out.toByteArray();
    }

    /**
     * 写出到输出流。
     *
     * @param out       目标流（由调用方负责关闭）
     * @param sheetName sheet 名称
     * @param rows      数据行
     * @param type      行模型类型
     * @param <T>       行模型类型
     * @throws ExcelException 行数超限或底层写出失败
     */
    public <T> void write(OutputStream out, String sheetName, List<T> rows, Class<T> type) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(sheetName, "sheetName");
        Objects.requireNonNull(type, "type");
        List<T> data = rows == null ? List.of() : rows;
        if (data.size() > maxRows) {
            throw new ExcelException(
                    "导出行数 " + data.size() + " 超过上限 " + maxRows + "，请分批导出");
        }
        try {
            EasyExcel.write(out, type).sheet(sheetName).doWrite(data);
        } catch (ExcelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ExcelException("Excel 导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 单次导出最大行数。
     *
     * @return 行数上限
     */
    public int getMaxRows() {
        return maxRows;
    }
}
