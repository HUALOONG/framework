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
import cn.jowen.framework.extras.operatelog.OperateStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingOperateLogHandlerTest {

    private final Logger logger = Logger.getLogger(LoggingOperateLogHandler.class.getName());
    private final CapturingHandler capturingHandler = new CapturingHandler();

    @BeforeEach
    void setUp() {
        logger.setLevel(Level.ALL);
        capturingHandler.setLevel(Level.ALL);
        logger.addHandler(capturingHandler);
    }

    @AfterEach
    void tearDown() {
        logger.removeHandler(capturingHandler);
    }

    @Test
    void handleLogsRecordWithoutThrowing() {
        OperateLogHandler handler = new LoggingOperateLogHandler();
        OperateLogRecord record = OperateLogRecord.builder()
                .module("用户管理").action("CREATE")
                .operator("admin").status(OperateStatus.SUCCESS).costTime(7L).build();

        handler.handle(record);

        assertThat(capturingHandler.records).isNotEmpty();
        assertThat(capturingHandler.records.get(0).getMessage())
                .contains("用户管理").contains("CREATE");
    }

    static final class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
