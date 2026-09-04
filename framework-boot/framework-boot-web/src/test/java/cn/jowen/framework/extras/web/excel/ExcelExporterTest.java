package cn.jowen.framework.extras.web.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ExcelExporter} 失败分支测试：
 * 覆盖底层写出失败时 {@code catch (RuntimeException)} 将异常包裹为 {@link ExcelException} 的分支。
 *
 * <p>正常导出、空/null 行与行数上限分支已由 {@link ExcelRoundTripTest} 覆盖，
 * 本测试仅聚焦 {@code write} 的异常包裹逻辑（EasyExcel 写出失败时抛 RuntimeException）。
 *
 * <p>注：{@code catch (ExcelException e) { throw e; }} 这一分支（ExcelExporter.java:86-87）
 * 在通过公共 API 调用时不可达——EasyExcel 会把写出过程中的任何异常（含原始
 * {@link ExcelException}）统一归一化为 {@code ExcelAnalysisException}/{@code ExcelGenerateException}
 * （二者均为 RuntimeException），因此只会落到 {@code catch (RuntimeException)} 分支。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ExcelExporterTest {

    /** 测试行模型。 */
    public static class Row {
        /** 姓名。 */
        @ExcelProperty("姓名")
        public String name;

        /** 分数。 */
        @ExcelProperty("分数")
        public Integer score;

        public Row() {
        }

        public Row(String name, Integer score) {
            this.name = name;
            this.score = score;
        }
    }

    @Test
    void write_streamWriteFails_wrapsAsExcelException() {
        ExcelExporter exporter = new ExcelExporter(1000);
        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("disk full");
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("disk full");
            }
        };

        assertThatThrownBy(() -> exporter.write(failing, "数据", List.of(new Row("a", 1)), Row.class))
                .isInstanceOf(ExcelException.class)
                .hasMessageContaining("Excel 导出失败");
    }

    @Test
    void write_delegateExcelException_normalizedThroughRuntimeExceptionBranch() {
        ExcelExporter exporter = new ExcelExporter(1000);
        // 即便写出端直接抛出 ExcelException，EasyExcel 仍会将其归一化为 ExcelAnalysisException
        // （RuntimeException），因此该异常落在 catch (RuntimeException) 分支被重新包裹。
        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) {
                throw new ExcelException("delegate boom");
            }
        };

        assertThatThrownBy(() -> exporter.write(failing, "数据", List.of(new Row("a", 1)), Row.class))
                .isInstanceOf(ExcelException.class)
                .hasMessageContaining("Excel 导出失败");
    }
}
