package cn.jowen.framework.extras.excel.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Field;

/**
 * 枚举转换器，支持通过枚举名（name）进行转换。
 *
 * @param <T> 枚举类型
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class EnumConverter<T extends Enum<T>> implements Converter<T> {

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> supportJavaTypeKey() {
        // 运行时确定，此处返回 Enum 的 Class
        return (Class<T>) (Class<?>) Enum.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                               GlobalConfiguration globalConfiguration) {
        String text = cellData.getStringValue();
        if (text == null || text.isBlank()) {
            return null;
        }
        // 通过反射获取枚举类型并查找
        Field field = contentProperty != null ? contentProperty.getField() : null;
        Class<?> javaType = field != null ? field.getType() : null;
        if (javaType == null || !javaType.isEnum()) {
            throw new IllegalArgumentException("Expected an enum type but got: " + javaType);
        }
        for (Object constant : javaType.getEnumConstants()) {
            if (constant instanceof Enum<?>) {
                Enum<?> e = (Enum<?>) constant;
                if (e.name().equalsIgnoreCase(text.trim())) {
                    return (T) e;
                }
            }
        }
        throw new IllegalArgumentException("No enum constant matching: " + text);
    }

    @Override
    public WriteCellData<String> convertToExcelData(T value, ExcelContentProperty contentProperty,
                                                    GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(value.name());
    }
}
