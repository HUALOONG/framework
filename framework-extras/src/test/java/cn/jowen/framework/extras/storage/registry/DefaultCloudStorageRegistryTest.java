/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.jowen.framework.extras.storage.FileInfo;
import cn.jowen.framework.extras.storage.FileStorage;
import org.junit.jupiter.api.Test;

/**
 * {@link DefaultCloudStorageRegistry} 基础行为测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class DefaultCloudStorageRegistryTest {

    @Test
    void getStorageReturnsNullWhenEmpty() {
        DefaultCloudStorageRegistry registry = new DefaultCloudStorageRegistry();
        assertEquals(null, registry.getStorage("missing"));
    }

    @Test
    void registerAndGetStorage() {
        DefaultCloudStorageRegistry registry = new DefaultCloudStorageRegistry();
        FileStorage mock = createMockStorage();
        registry.registerStorage("minio", mock);
        assertNotNull(registry.getStorage("minio"));
        assertEquals(mock, registry.getStorage("minio"));
    }

    @Test
    void storageNamesReturnsSet() {
        DefaultCloudStorageRegistry registry = new DefaultCloudStorageRegistry();
        assertTrue(registry.storageNames().isEmpty());
        registry.registerStorage("minio", createMockStorage());
        registry.registerStorage("oss", createMockStorage());
        assertEquals(2, registry.storageNames().size());
        assertTrue(registry.storageNames().contains("minio"));
        assertTrue(registry.storageNames().contains("oss"));
    }

    @Test
    void registerNullNameThrows() {
        DefaultCloudStorageRegistry registry = new DefaultCloudStorageRegistry();
        assertThrows(NullPointerException.class, () -> registry.registerStorage(null, createMockStorage()));
    }

    @Test
    void registerNullStorageThrows() {
        DefaultCloudStorageRegistry registry = new DefaultCloudStorageRegistry();
        assertThrows(NullPointerException.class, () -> registry.registerStorage("s3", null));
    }

    private FileStorage createMockStorage() {
        return new FileStorage() {
            @Override
            public FileInfo upload(java.io.InputStream in, String originalFilename) { return null; }
            @Override
            public java.io.InputStream download(String objectName) { return null; }
            @Override
            public String getPresignedUrl(String objectName) { return null; }
            @Override
            public void delete(String objectName) {}
            @Override
            public void deleteBatch(java.util.List<String> names) {}
            @Override
            public boolean exists(String objectName) { return false; }
            @Override
            public FileInfo getFileInfo(String objectName) { return null; }
            @Override
            public java.util.List<FileInfo> listObjects(String prefix) { return java.util.List.of(); }
        };
    }
}
