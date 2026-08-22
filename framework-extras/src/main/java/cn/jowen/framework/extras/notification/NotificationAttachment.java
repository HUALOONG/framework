/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 通知附件。
 *
 * @param name        文件名
 * @param contentType 内容类型（可为 null）
 * @param content     字节内容
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public record NotificationAttachment(String name, @Nullable String contentType, byte[] content) {

    public NotificationAttachment {
        Objects.requireNonNull(name, "name must not be null");
        content = content == null ? new byte[0] : content;
    }
}
