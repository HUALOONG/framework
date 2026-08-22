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
import cn.jowen.framework.extras.storage.StorageException;
import cn.jowen.framework.extras.storage.strategy.ObjectNameStrategy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 本地磁盘文件存储：基于 {@code java.nio.file}，按命名策略生成对象名并落盘。
 *
 * <p>零外部依赖。对象名经过路径穿越校验，禁止 {@code ..} 与越界访问。
 * {@link #getPresignedUrl(String)} 返回 {@code null}（本地方略不支持预签名）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class LocalFileStorage implements FileStorage {

    private static final Map<String, String> EXT_CONTENT_TYPES = Map.ofEntries(
            Map.entry("txt", "text/plain"),
            Map.entry("html", "text/html"),
            Map.entry("css", "text/css"),
            Map.entry("js", "application/javascript"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("zip", "application/zip"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("mp4", "video/mp4"));

    private final Path rootDir;
    private final ObjectNameStrategy naming;

    public LocalFileStorage(Path rootDir, ObjectNameStrategy naming) {
        this.rootDir = Objects.requireNonNull(rootDir, "rootDir must not be null");
        this.naming = Objects.requireNonNull(naming, "naming must not be null");
    }

    @Override
    public FileInfo upload(InputStream in, String originalFilename) {
        Objects.requireNonNull(in, "input stream must not be null");
        String objectName = naming.generate(originalFilename);
        Path target = resolve(objectName);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("文件上传失败: " + objectName, e);
        }
        String baseName = originalFilename == null ? target.getFileName().toString() : originalFilename;
        return buildFileInfo(objectName, baseName);
    }

    @Override
    public InputStream download(String objectName) {
        Path target = resolve(objectName);
        if (!Files.exists(target)) {
            throw new StorageException("文件不存在: " + objectName);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new StorageException("文件打开失败: " + objectName, e);
        }
    }

    @Override
    public @Nullable String getPresignedUrl(String objectName) {
        // 本地方略不支持预签名 URL；下载请使用 download()
        return null;
    }

    @Override
    public void delete(String objectName) {
        try {
            Files.deleteIfExists(resolve(objectName));
        } catch (IOException e) {
            throw new StorageException("文件删除失败: " + objectName, e);
        }
    }

    @Override
    public void deleteBatch(List<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            delete(name);
        }
    }

    @Override
    public boolean exists(String objectName) {
        return Files.exists(resolve(objectName));
    }

    @Override
    public @Nullable FileInfo getFileInfo(String objectName) {
        Path target = resolve(objectName);
        if (!Files.exists(target)) {
            return null;
        }
        try {
            java.nio.file.attribute.BasicFileAttributes attrs =
                    Files.readAttributes(target, java.nio.file.attribute.BasicFileAttributes.class);
            return new FileInfo(objectName, target.getFileName().toString(),
                    contentType(target), attrs.size(), etag(attrs), attrs.lastModifiedTime().toMillis(),
                    null, Map.of());
        } catch (IOException e) {
            throw new StorageException("读取文件元信息失败: " + objectName, e);
        }
    }

    @Override
    public List<FileInfo> listObjects(@Nullable String prefix) {
        List<FileInfo> result = new ArrayList<>();
        if (!Files.exists(rootDir)) {
            return result;
        }
        String normalizedPrefix = prefix == null ? "" : prefix;
        try (var stream = Files.walk(rootDir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String objectName = rootDir.relativize(path).toString().replace('\\', '/');
                if (objectName.startsWith(normalizedPrefix)) {
                    FileInfo info = buildFileInfo(objectName, objectName);
                    result.add(info);
                }
            });
        } catch (IOException e) {
            throw new StorageException("列举文件失败", e);
        }
        return result;
    }

    private FileInfo buildFileInfo(String objectName, String originalFilename) {
        Path target = resolve(objectName);
        try {
            java.nio.file.attribute.BasicFileAttributes attrs =
                    Files.readAttributes(target, java.nio.file.attribute.BasicFileAttributes.class);
            return new FileInfo(objectName, originalFilename, contentType(target), attrs.size(),
                    etag(attrs), attrs.lastModifiedTime().toMillis(), null, Map.of());
        } catch (IOException e) {
            throw new StorageException("读取文件元信息失败: " + objectName, e);
        }
    }

    private static String contentType(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            String ext = name.substring(dot + 1);
            String type = EXT_CONTENT_TYPES.get(ext);
            if (type != null) {
                return type;
            }
        }
        return "application/octet-stream";
    }

    private static String etag(java.nio.file.attribute.BasicFileAttributes attrs) {
        return Long.toHexString(attrs.size()) + "-" + Long.toHexString(attrs.lastModifiedTime().toMillis());
    }

    private Path resolve(String objectName) {
        Objects.requireNonNull(objectName, "objectName must not be null");
        if (objectName.isBlank() || objectName.contains("..")) {
            throw new StorageException("非法对象名: " + objectName);
        }
        Path resolved = rootDir.resolve(objectName).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new StorageException("对象名越界: " + objectName);
        }
        return resolved;
    }
}
