package cn.jowen.framework.extras.excel.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * LocalDate 转换器，支持 yyyy-MM-dd 和 yyyy/MM/dd 格式。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class LocalDateConverter implements Converter<LocalDate> {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Class<LocalDate> supportJavaTypeKey() {
        return LocalDate.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public LocalDate convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                       GlobalConfiguration globalConfiguration) {
        String text = cellData.getStringValue();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DEFAULT_FORMATTER);
        } catch (DateTimeParseException e) {
            // 尝试另一种格式
            try {
                return LocalDate.parse(text.trim().replace('/', '-'), DEFAULT_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Cannot parse LocalDate from: " + text, ex);
            }
        }
    }

    @Override
    public WriteCellData<?> convertToExcelData(LocalDate value, ExcelContentProperty contentProperty,
                                               GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(value.format(DEFAULT_FORMATTER));
    }
}
