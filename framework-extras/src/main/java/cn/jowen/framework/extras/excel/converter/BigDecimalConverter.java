package cn.jowen.framework.extras.excel.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * BigDecimal 转换器。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class BigDecimalConverter implements Converter<BigDecimal> {

    @Override
    public Class<BigDecimal> supportJavaTypeKey() {
        return BigDecimal.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.NUMBER;
    }

    @Override
    public @Nullable BigDecimal convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                                  GlobalConfiguration globalConfiguration) {
        if (cellData.getType() == CellDataTypeEnum.NUMBER) {
            return cellData.getNumberValue();
        }
        String text = cellData.getStringValue();
        if (text == null || text.isBlank()) {
            return null;
        }
        return new BigDecimal(text.trim());
    }

    @Override
    public WriteCellData<BigDecimal> convertToExcelData(BigDecimal value, ExcelContentProperty contentProperty,
                                                        GlobalConfiguration globalConfiguration) {
        return new WriteCellData<>(value);
    }
}
