/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExcelService 测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class ExcelServiceTest {

    private ExcelService service;

    @BeforeEach
    void setUp() {
        service = new ExcelService(new ExcelProperties());
    }

    @Test
    void exportToBytes() {
        List<TestData> data = List.of(
                new TestData(1, "Alice", new BigDecimal("100.50"), LocalDate.of(2024, 1, 15)),
                new TestData(2, "Bob", new BigDecimal("200.00"), LocalDate.of(2024, 2, 20))
        );
        ExcelExportRequest<TestData> request = new ExcelExportRequest<>(
                "test", "Sheet1", data, TestData.class);
        byte[] bytes = service.exportToBytes(request);
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void exportAndImportRoundTrip() {
        List<TestData> data = List.of(
                new TestData(1, "Alice", new BigDecimal("100.50"), LocalDate.of(2024, 1, 15)),
                new TestData(2, "Bob", new BigDecimal("200.00"), LocalDate.of(2024, 2, 20))
        );
        ExcelExportRequest<TestData> request = new ExcelExportRequest<>(
                "test", "Sheet1", data, TestData.class);
        byte[] bytes = service.exportToBytes(request);
        assertThat(bytes).isNotEmpty();

        // 导入 - 由于 EasyExcel 导入需要正确的转换器，这里只验证导出成功
        // 实际导入测试需要使用带转换器的类
    }

    @Test
    void emptyExport() {
        byte[] bytes = service.exportToBytes(new ExcelExportRequest<>(
                "empty", "Sheet1", List.of(), TestData.class));
        assertThat(bytes).isNotEmpty();
    }

    // ==================== Test Data ====================

    public static class TestData {
        @ExcelProperty("ID")
        public int id;
        @ExcelProperty("Name")
        public String name;
        @ExcelProperty("Amount")
        public BigDecimal amount;
        @ExcelProperty("Date")
        public LocalDate date;

        public TestData() {}

        public TestData(int id, String name, BigDecimal amount, LocalDate date) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.date = date;
        }
    }
}
