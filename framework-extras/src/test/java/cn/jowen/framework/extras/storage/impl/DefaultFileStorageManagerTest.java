/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.jowen.framework.extras.storage.FileInfo;
import cn.jowen.framework.extras.storage.FileStorage;
import org.junit.jupiter.api.Test;

/**
 * {@link DefaultFileStorageManager} 基础行为测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
class DefaultFileStorageManagerTest {

    @Test
    void getStorageReturnsDefault() {
        FileStorage mockStorage = createMockStorage();
        DefaultFileStorageManager manager = new DefaultFileStorageManager("test-default");
        manager.registerStorage("test-default", mockStorage);
        assertNotNull(manager.getStorage());
        assertEquals(mockStorage, manager.getStorage());
    }

    @Test
    void getStorageByName() {
        FileStorage s3 = createMockStorage();
        DefaultFileStorageManager manager = new DefaultFileStorageManager("local");
        manager.registerStorage("s3", s3);
        assertNotNull(manager.getStorage("s3"));
        assertEquals(s3, manager.getStorage("s3"));
    }

    @Test
    void getStorageByNameReturnsNullWhenMissing() {
        DefaultFileStorageManager manager = new DefaultFileStorageManager("local");
        assertEquals(null, manager.getStorage("missing"));
    }

    @Test
    void getStorageThrowsWhenDefaultNotRegistered() {
        DefaultFileStorageManager manager = new DefaultFileStorageManager("local");
        assertThrows(IllegalStateException.class, manager::getStorage);
    }

    @Test
    void registerStorageNullNameThrows() {
        DefaultFileStorageManager manager = new DefaultFileStorageManager("local");
        assertThrows(NullPointerException.class, () -> manager.registerStorage(null, createMockStorage()));
    }

    @Test
    void registerStorageNullStorageThrows() {
        DefaultFileStorageManager manager = new DefaultFileStorageManager("local");
        assertThrows(NullPointerException.class, () -> manager.registerStorage("s3", null));
    }

    private FileStorage createMockStorage() {
        return new FileStorage() {
            @Override
            public FileInfo upload(java.io.InputStream in, String originalFilename) {
                return null;
            }
            @Override
            public java.io.InputStream download(String objectName) {
                return null;
            }
            @Override
            public String getPresignedUrl(String objectName) {
                return null;
            }
            @Override
            public void delete(String objectName) {}
            @Override
            public void deleteBatch(java.util.List<String> names) {}
            @Override
            public boolean exists(String objectName) {
                return false;
            }
            @Override
            public FileInfo getFileInfo(String objectName) {
                return null;
            }
            @Override
            public java.util.List<FileInfo> listObjects(String prefix) {
                return java.util.List.of();
            }
        };
    }
}
