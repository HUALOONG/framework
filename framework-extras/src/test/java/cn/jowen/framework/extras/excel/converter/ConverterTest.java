/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.excel.converter;

import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内置转换器测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class ConverterTest {

    private final GlobalConfiguration globalConfig = new GlobalConfiguration();

    // ==================== LocalDateConverter ====================

    @Test
    void localDateConvertFromString() {
        LocalDateConverter converter = new LocalDateConverter();
        ReadCellData<String> cellData = new ReadCellData<>("2024-01-15");
        LocalDate result = converter.convertToJavaData(cellData, null, globalConfig);
        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void localDateConvertSlashFormat() {
        LocalDateConverter converter = new LocalDateConverter();
        ReadCellData<String> cellData = new ReadCellData<>("2024/03/20");
        LocalDate result = converter.convertToJavaData(cellData, null, globalConfig);
        assertThat(result).isEqualTo(LocalDate.of(2024, 3, 20));
    }

    @Test
    void localDateConvertEmptyReturnsNull() {
        LocalDateConverter converter = new LocalDateConverter();
        ReadCellData<String> cellData = new ReadCellData<>("");
        assertThat(converter.convertToJavaData(cellData, null, globalConfig)).isNull();
    }

    @Test
    void localDateConvertToExcel() {
        LocalDateConverter converter = new LocalDateConverter();
        WriteCellData<?> result = converter.convertToExcelData(LocalDate.of(2024, 6, 1), null, globalConfig);
        assertThat(result.getStringValue()).isEqualTo("2024-06-01");
    }

    // ==================== LocalDateTimeConverter ====================

    @Test
    void localDateTimeConvert() {
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        ReadCellData<String> cellData = new ReadCellData<>("2024-01-15 10:30:00");
        LocalDateTime result = converter.convertToJavaData(cellData, null, globalConfig);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    }

    // ==================== BigDecimalConverter ====================

    @Test
    void bigDecimalConvertFromNumber() {
        BigDecimalConverter converter = new BigDecimalConverter();
        ReadCellData<BigDecimal> cellData = new ReadCellData<>(new BigDecimal("123.45"));
        BigDecimal result = converter.convertToJavaData(cellData, null, globalConfig);
        assertThat(result).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    void bigDecimalConvertFromString() {
        BigDecimalConverter converter = new BigDecimalConverter();
        ReadCellData<String> cellData = new ReadCellData<>("99.99");
        BigDecimal result = converter.convertToJavaData(cellData, null, globalConfig);
        assertThat(result).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    // ==================== EnumConverter ====================

    @Test
    void enumConvertByName() throws NoSuchFieldException {
        EnumConverter<TestStatus> converter = new EnumConverter<>();
        ReadCellData<String> cellData = new ReadCellData<>("ACTIVE");
        ExcelContentProperty prop = new ExcelContentProperty();
        prop.setField(TestData.class.getField("status"));
        TestStatus result = converter.convertToJavaData(cellData, prop, globalConfig);
        assertThat(result).isEqualTo(TestStatus.ACTIVE);
    }

    @Test
    void enumConvertIgnoreCase() throws NoSuchFieldException {
        EnumConverter<TestStatus> converter = new EnumConverter<>();
        ReadCellData<String> cellData = new ReadCellData<>("active");
        ExcelContentProperty prop = new ExcelContentProperty();
        prop.setField(TestData.class.getField("status"));
        TestStatus result = converter.convertToJavaData(cellData, prop, globalConfig);
        assertThat(result).isEqualTo(TestStatus.ACTIVE);
    }

    // ==================== DictConverter ====================

    @Test
    void dictConvertByText() {
        Map<Integer, String> dict = Map.of(1, "男", 2, "女");
        DictConverter converter = new DictConverter(dict);
        ReadCellData<String> cellData = new ReadCellData<>("男");
        String result = converter.convertToJavaData(cellData, null, globalConfig);
        assertThat(result).isEqualTo("男");
    }

    @Test
    void dictGetLabel() {
        Map<Integer, String> dict = Map.of(1, "男", 2, "女");
        DictConverter converter = new DictConverter(dict);
        assertThat(converter.getLabel(1)).isEqualTo("男");
        assertThat(converter.getLabel(2)).isEqualTo("女");
        assertThat(converter.getLabel(99)).isNull();
    }

    @Test
    void dictGetCode() {
        Map<Integer, String> dict = Map.of(1, "男", 2, "女");
        DictConverter converter = new DictConverter(dict);
        assertThat(converter.getCode("男")).isEqualTo(1);
        assertThat(converter.getCode("女")).isEqualTo(2);
        assertThat(converter.getCode("unknown")).isNull();
    }

    // ==================== Test Classes ====================

    enum TestStatus {
        ACTIVE, INACTIVE
    }

    static class TestData {
        public TestStatus status;
    }
}
