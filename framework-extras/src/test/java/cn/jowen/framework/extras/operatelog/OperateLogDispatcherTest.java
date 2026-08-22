/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.operatelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link OperateLogDispatcher} 基础行为测试，覆盖 {@code dispatchSync} 方法。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class OperateLogDispatcherTest {

    @Test
    void dispatchSyncCallsAllHandlers() {
        List<OperateLogRecord> received = new ArrayList<>();
        OperateLogHandler handler = received::add;
        OperateLogDispatcher dispatcher = new OperateLogDispatcher(handler);

        OperateLogRecord record = OperateLogRecord.builder()
                .module("test")
                .action("create")
                .content("payload")
                .build();
        dispatcher.dispatchSync(record);

        assertEquals(1, received.size());
        assertNotNull(received.get(0));
        assertEquals("test", received.get(0).module());
    }

    @Test
    void dispatchSyncWithMultipleHandlers() {
        List<OperateLogRecord> h1 = new ArrayList<>();
        List<OperateLogRecord> h2 = new ArrayList<>();
        OperateLogDispatcher dispatcher = new OperateLogDispatcher(
                h1::add, h2::add);

        OperateLogRecord record = OperateLogRecord.builder()
                .module("m")
                .action("a")
                .build();
        dispatcher.dispatchSync(record);

        assertEquals(1, h1.size());
        assertEquals(1, h2.size());
    }

    @Test
    void dispatchSyncNullRecordThrows() {
        OperateLogDispatcher dispatcher = new OperateLogDispatcher();
        assertThrows(NullPointerException.class, () -> dispatcher.dispatchSync(null));
    }

    @Test
    void dispatchAsyncSubmitsToVirtualThreadExecutor() throws InterruptedException {
        List<OperateLogRecord> received = new ArrayList<>();
        OperateLogDispatcher dispatcher = new OperateLogDispatcher(received::add);

        OperateLogRecord record = OperateLogRecord.builder()
                .module("m")
                .action("a")
                .build();
        dispatcher.dispatch(record);

        // 异步，等待虚拟线程执行完成
        Thread.sleep(200);
        assertEquals(1, received.size());
        dispatcher.shutdown();
    }

    @Test
    void dispatchNullRecordThrows() {
        OperateLogDispatcher dispatcher = new OperateLogDispatcher();
        assertThrows(NullPointerException.class, () -> dispatcher.dispatch(null));
    }
}
