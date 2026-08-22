/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.operatelog;

import org.jspecify.annotations.NullMarked;

/**
 * 操作日志处理器（SPI）：消费一条 {@link OperateLogRecord}。实现可为本地日志、数据库、消息队列等。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public interface OperateLogHandler {

    /**
     * 处理操作日志。
     *
     * @param record 日志记录（不可为 null）
     */
    void handle(OperateLogRecord record);
}
