/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 文件元信息。
 *
 * @param objectName     对象名（存储内唯一路径）
 * @param originalFilename 原始文件名（最佳努力推断）
 * @param contentType   内容类型
 * @param size          字节大小
 * @param etag          实体标签（弱校验，基于大小+修改时间）
 * @param lastModified  最后修改时间（毫秒）
 * @param url           可访问 URL（本地为 null）
 * @param metadata      自定义元数据
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public record FileInfo(String objectName, @Nullable String originalFilename, @Nullable String contentType,
        long size, @Nullable String etag, long lastModified, @Nullable String url,
        @Nullable Map<String, String> metadata) {

    public FileInfo {
        Objects.requireNonNull(objectName, "objectName must not be null");
        metadata = metadata == null ? Map.of() : metadata;
    }
}
