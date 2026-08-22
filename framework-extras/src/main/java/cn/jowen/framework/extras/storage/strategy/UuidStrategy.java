/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.strategy;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * UUID 命名策略：生成 {@code <uuid><扩展名>} 形式对象名（保留原扩展名）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class UuidStrategy implements ObjectNameStrategy {

    @Override
    public String generate(@Nullable String originalFilename) {
        String base = DatePathStrategy.basename(originalFilename);
        String ext = extension(base);
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            return name.substring(dot);
        }
        return "";
    }
}
