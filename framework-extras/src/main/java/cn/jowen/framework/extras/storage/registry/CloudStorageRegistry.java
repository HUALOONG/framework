/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.registry;

import cn.jowen.framework.extras.storage.FileStorage;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 云存储注册表 SPI 接口。
 *
 * <p>聚合所有已注册的云后端 {@link FileStorage}，支持按名称获取。
 * 由 {@code ExtrasBootstrapConfiguration} 统一注入并调用 {@code manager.registerStorage()} 填充。
 *
 * @author Jowen
 * @date 2026-08-22
 * @see DefaultCloudStorageRegistry
 */
@NullMarked
public interface CloudStorageRegistry {

    /**
     * 按名称获取云存储实例。
     *
     * @param name 存储名称
     * @return 存储实例，不存在返回 {@code null}
     */
    @Nullable
    FileStorage getStorage(String name);

    /**
     * 注册云存储实例。
     *
     * @param name    存储名称
     * @param storage 存储实例
     */
    void registerStorage(String name, FileStorage storage);

    /**
     * 返回所有已注册存储名称。
     *
     * @return 名称集合，不可为 {@code null}
     */
    Set<String> storageNames();
}
