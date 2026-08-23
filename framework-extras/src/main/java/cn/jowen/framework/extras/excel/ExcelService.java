package cn.jowen.framework.extras.excel;

import cn.jowen.framework.extras.config.ExcelProperties;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 处理主服务，提供导出（到流/字节数组）、导入、模板填充三种入口。
 *
 * <p>导入结果通过 {@link ExcelImportResult} 返回，包含成功数据与错误信息。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
@Service
@EnableConfigurationProperties(ExcelProperties.class)
public class ExcelService {

    private static final Logger log = LoggerFactory.getLogger(ExcelService.class);

    private final ExcelProperties properties;

    public ExcelService(ExcelProperties properties) {
        this.properties = properties;
    }

    /**
     * 导出数据到输出流。
     *
     * @param request      导出请求
     * @param outputStream 输出流
     */
    public void export(ExcelExportRequest<?> request, OutputStream outputStream) {
        EasyExcel.write(outputStream, request.head())
                .sheet(request.sheetName())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .doWrite(request.data());
    }

    /**
     * 导出数据为字节数组。
     *
     * @param request 导出请求
     * @return 字节数组
     */
    public byte[] exportToBytes(ExcelExportRequest<?> request) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        export(request, bos);
        return bos.toByteArray();
    }

    /**
     * 从输入流导入数据，返回导入结果。
     *
     * @param request 导入请求
     * @return 导入结果
     */
    public <T> ExcelImportResult<T> importData(ExcelImportRequest<T> request) {
        ExcelImportListener<T> listener = new ExcelImportListener<>(request.validator(), request.head());
        try {
            EasyExcel.read(request.inputStream(), request.head(), listener)
                    .sheet(request.sheetNo())
                    .headRowNumber(request.headerRowNumber())
                    .doRead();
        } catch (Exception e) {
            log.error("excel import failed", e);
            throw new IllegalStateException("excel import failed", e);
        }
        List<T> data = listener.getData();
        List<ExcelError> errors = listener.getErrors();
        int totalCount = data.size() + errors.size();
        return new ExcelImportResult<>(data, errors, totalCount, data.size(), errors.size());
    }

    /**
     * 使用模板填充数据并输出到流。
     *
     * @param template     模板输入流
     * @param data         填充数据（Map 或对象）
     * @param outputStream 输出流
     */
    public void fill(InputStream template, Object data, OutputStream outputStream) {
        EasyExcel.write(outputStream)
                .withTemplate(template)
                .sheet()
                .doFill(data);
    }

    /**
     * 导入监听器实现，收集数据和错误。
     */
    private static class ExcelImportListener<T> implements ReadListener<T> {

        private final ExcelRowValidator<T> validator;
        private final Class<T> head;
        private final List<T> data = new ArrayList<>();
        private final List<ExcelError> errors = new ArrayList<>();
        private int rowIndex = 0;

        ExcelImportListener(@Nullable ExcelRowValidator<T> validator, Class<T> head) {
            this.validator = validator;
            this.head = head;
        }

        @Override
        public void invoke(T t, AnalysisContext analysisContext) {
            int rownum = analysisContext.readRowHolder().getRowIndex();
            if (validator != null && !validator.validate(t, rownum)) {
                errors.add(new ExcelError(rownum, -1, null, null, "row validation failed"));
                return;
            }
            data.add(t);
            rowIndex++;
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            log.info("excel import done, total={}, success={}, fail={}", data.size() + errors.size(), data.size(), errors.size());
        }

        List<T> getData() {
            return data;
        }

        List<ExcelError> getErrors() {
            return errors;
        }
    }
}
