/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.impl;

import cn.jowen.framework.extras.storage.FileInfo;
import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.strategy.DatePathStrategy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private FileStorage newStorage() {
        return new LocalFileStorage(tempDir, new DatePathStrategy());
    }

    private static InputStream content(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uploadThenExistsAndInfo() {
        FileStorage storage = newStorage();
        FileInfo info = storage.upload(content("hello world"), "demo.txt");

        assertThat(storage.exists(info.objectName())).isTrue();
        FileInfo fetched = storage.getFileInfo(info.objectName());
        assertThat(fetched).isNotNull();
        assertThat(fetched.size()).isEqualTo("hello world".getBytes(StandardCharsets.UTF_8).length);
        assertThat(fetched.contentType()).isEqualTo("text/plain");
    }

    @Test
    void downloadReturnsSameContent() throws IOException {
        FileStorage storage = newStorage();
        FileInfo info = storage.upload(content("framework-extras"), "data.txt");

        try (InputStream in = storage.download(info.objectName());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("framework-extras");
        }
    }

    @Test
    void deleteRemovesObject() {
        FileStorage storage = newStorage();
        FileInfo info = storage.upload(content("to-delete"), "x.txt");

        assertThat(storage.exists(info.objectName())).isTrue();
        storage.delete(info.objectName());
        assertThat(storage.exists(info.objectName())).isFalse();
    }

    @Test
    void deleteBatchRemovesMultiple() {
        FileStorage storage = newStorage();
        FileInfo a = storage.upload(content("a"), "a.txt");
        FileInfo b = storage.upload(content("b"), "b.txt");

        storage.deleteBatch(java.util.List.of(a.objectName(), b.objectName()));
        assertThat(storage.exists(a.objectName())).isFalse();
        assertThat(storage.exists(b.objectName())).isFalse();
    }

    @Test
    void getPresignedUrlReturnsNull() {
        FileStorage storage = newStorage();
        assertThat(storage.getPresignedUrl("any-object")).isNull();
    }

    @Test
    void listObjectsFiltersByPrefix() {
        FileStorage storage = newStorage();
        FileInfo a = storage.upload(content("1"), "aaa_img.png");
        storage.upload(content("2"), "bbb_doc.txt");
        storage.upload(content("3"), "ccc.txt");

        // DatePathStrategy 以当前日期生成 yyyy/MM/dd/ 前缀，三者同目录
        String today = java.time.LocalDate.now().toString().replace('-', '/') + "/";
        assertThat(storage.listObjects(today)).hasSize(3);
        assertThat(storage.listObjects("")).hasSize(3);
        // 精确前缀（对象全名）只命中自身
        assertThat(storage.listObjects(a.objectName())).hasSize(1);
        // 不匹配前缀返回空
        assertThat(storage.listObjects("2099/01/01/")).isEmpty();
    }
}
