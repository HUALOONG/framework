/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.strategy;

import org.jspecify.annotations.NullMarked;

/**
 * 对象命名策略：将原始文件名映射为存储对象名（可含层级路径）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public interface ObjectNameStrategy {

    /**
     * 生成对象名。
     *
     * @param originalFilename 原始文件名（可能为 null 或含路径）
     * @return 对象名，不可为 {@code null}
     */
    String generate(String originalFilename);
}
