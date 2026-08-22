/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.strategy;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 原名命名策略：保留原始文件名（去除路径分隔符），适合已保证唯一性的场景。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class OriginalNameStrategy implements ObjectNameStrategy {

    @Override
    public String generate(@Nullable String originalFilename) {
        return DatePathStrategy.basename(originalFilename);
    }
}
