/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.operatelog.handler;

import cn.jowen.framework.extras.operatelog.OperateLogHandler;
import cn.jowen.framework.extras.operatelog.OperateLogRecord;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;

/**
 * 默认操作日志处理器：将记录格式化后写入 JDK 日志（{@link Logger}）。
 *
 * <p>零外部依赖（使用 {@code java.util.logging}），可直接用于本地调试；
 * 生产持久化可扩展 {@link OperateLogHandler} 自行实现。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class LoggingOperateLogHandler implements OperateLogHandler {

    private final Logger logger = Logger.getLogger(LoggingOperateLogHandler.class.getName());

    @Override
    public void handle(OperateLogRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        logger.log(Level.INFO, format(record));
    }

    private static String format(OperateLogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("[OPERATE-LOG] module=").append(record.module())
                .append(", action=").append(record.action())
                .append(", status=").append(record.status());
        if (record.operator() != null) {
            sb.append(", operator=").append(record.operator());
        }
        if (record.description() != null && !record.description().isEmpty()) {
            sb.append(", description=").append(record.description());
        }
        if (record.content() != null && !record.content().isEmpty()) {
            sb.append(", content=").append(record.content());
        }
        sb.append(", costTime=").append(record.costTime()).append("ms");
        return sb.toString();
    }
}
