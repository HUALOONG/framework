/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage;

import java.io.InputStream;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 文件存储抽象接口。
 *
 * <p>统一本地磁盘与云后端（本轮仅 {@code LocalFileStorage} 实现），所有方法零/低外部依赖。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public interface FileStorage {

    /**
     * 上传文件。
     *
     * @param in               输入流（调用方负责关闭）
     * @param originalFilename 原始文件名（用于命名策略与内容类型推断）
     * @return 文件元信息
     */
    FileInfo upload(InputStream in, String originalFilename);

    /**
     * 下载文件。
     *
     * @param objectName 对象名
     * @return 文件输入流
     * @throws StorageException 对象不存在时
     */
    InputStream download(String objectName);

    /**
     * 生成预签名 URL。
     *
     * <p>本地方略（{@code LocalFileStorage}）不支持预签名，返回 {@code null}；
     * 下载请使用 {@link #download(String)}。
     *
     * @param objectName 对象名
     * @return 预签名 URL，不支持时返回 {@code null}
     */
    @Nullable
    String getPresignedUrl(String objectName);

    /** 删除单个对象。 */
    void delete(String objectName);

    /** 批量删除。 */
    void deleteBatch(List<String> names);

    /** 判断对象是否存在。 */
    boolean exists(String objectName);

    /**
     * 获取文件元信息。
     *
     * @param objectName 对象名
     * @return 元信息，不存在返回 {@code null}
     */
    @Nullable
    FileInfo getFileInfo(String objectName);

    /**
     * 列举对象（按前缀过滤）。
     *
     * @param prefix 前缀（null 或空表示匹配全部）
     * @return 对象元信息列表，不可为 {@code null}
     */
    List<FileInfo> listObjects(@Nullable String prefix);
}
