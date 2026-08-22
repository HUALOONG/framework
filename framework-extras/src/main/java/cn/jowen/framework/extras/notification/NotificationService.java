/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;

/**
 * 通知服务接口。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public interface NotificationService {

    /**
     * 同步发送。
     *
     * @param req 请求
     * @return 结果（未注册渠道返回 {@code success=false}，不抛异常）
     */
    NotificationResult send(NotificationRequest req);

    /**
     * 异步发送。
     *
     * @param req 请求
     * @return 完成后的结果 Future
     */
    CompletableFuture<NotificationResult> sendAsync(NotificationRequest req);

    /**
     * 批量发送（每条独立，部分失败不影响其他）。
     *
     * @param reqs 请求集合
     * @return 每条对应的结果列表
     */
    List<NotificationResult> sendBatch(List<NotificationRequest> reqs);
}
