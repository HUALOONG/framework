/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.excel;

import java.io.InputStream;
import org.jspecify.annotations.NullMarked;

/**
 * Excel 导入请求参数。
 *
 * @param <T> 数据行类型
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public record ExcelImportRequest<T>(
        /** Excel 文件输入流。 */
        InputStream inputStream,
        /** 数据行对应的 Class。 */
        Class<T> head,
        /** 要读取的 sheet 编号（从 0 开始，默认 0）。 */
        int sheetNo,
        /** 表头行数（默认 1）。 */
        int headerRowNumber,
        /** 行校验器，可为 null。 */
        ExcelRowValidator<T> validator,
        /** 批次大小（默认 500）。 */
        int batchSize
) {

    public ExcelImportRequest(InputStream inputStream, Class<T> head) {
        this(inputStream, head, 0, 1, null, 500);
    }
}
