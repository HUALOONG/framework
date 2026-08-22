/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.excel.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 字典转换器，将数字编码映射为显示文本（用于数据字典场景）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class DictConverter implements Converter<String> {

    private final Map<Integer, String> dictMap;
    private final Map<String, Integer> reverseDictMap;

    /**
     * 构造字典转换器。
     *
     * @param dictMap 编码 -> 显示文本
     */
    public DictConverter(Map<Integer, String> dictMap) {
        this.dictMap = dictMap == null ? Map.of() : dictMap;
        this.reverseDictMap = buildReverseMap(this.dictMap);
    }

    @Override
    public Class<String> supportJavaTypeKey() {
        return String.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public String convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
            GlobalConfiguration globalConfiguration) {
        // 导入时：从单元格文本查找编码，返回编码整数值（供业务层使用）
        String text = cellData.getStringValue();
        if (text == null || text.isBlank()) {
            return null;
        }
        // 查找编码对应的显示文本
        for (Map.Entry<Integer, String> entry : dictMap.entrySet()) {
            if (entry.getValue().equals(text.trim())) {
                return entry.getValue();
            }
        }
        return text.trim();
    }

    @Override
    public WriteCellData<?> convertToExcelData(String value, ExcelContentProperty contentProperty,
            GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(value);
    }

    /**
     * 根据编码获取显示文本。
     *
     * @param code 编码
     * @return 显示文本，未找到时返回 null
     */
    @Nullable
    public String getLabel(Integer code) {
        return dictMap.get(code);
    }

    /**
     * 根据显示文本获取编码。
     *
     * @param label 显示文本
     * @return 编码，未找到时返回 null
     */
    @Nullable
    public Integer getCode(String label) {
        return reverseDictMap.get(label);
    }

    private static Map<String, Integer> buildReverseMap(Map<Integer, String> map) {
        Map<String, Integer> result = new java.util.HashMap<>();
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }
}
