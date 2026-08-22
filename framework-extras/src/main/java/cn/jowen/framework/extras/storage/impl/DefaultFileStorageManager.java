/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.impl;

import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.FileStorageManager;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 默认文件存储器管理器实现。
 *
 * <p>维护命名存储实例，支持按名称获取与注册；默认存储固定为构造时传入的名称。
 * 线程安全（内部使用 {@code ConcurrentHashMap}）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class DefaultFileStorageManager implements FileStorageManager {

    private final ConcurrentHashMap<String, FileStorage> storages = new ConcurrentHashMap<>();
    private final String defaultName;

    public DefaultFileStorageManager(String defaultName) {
        this.defaultName = java.util.Objects.requireNonNull(defaultName, "defaultName must not be null");
    }

    @Override
    public FileStorage getStorage() {
        FileStorage storage = storages.get(defaultName);
        if (storage == null) {
            throw new IllegalStateException("default storage '" + defaultName + "' not registered");
        }
        return storage;
    }

    @Override
    public @Nullable FileStorage getStorage(String name) {
        return storages.get(name);
    }

    @Override
    public void registerStorage(String name, FileStorage storage) {
        java.util.Objects.requireNonNull(name, "name must not be null");
        java.util.Objects.requireNonNull(storage, "storage must not be null");
        storages.put(name, storage);
    }
}
