/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage;

import cn.jowen.framework.core.exception.FrameworkException;
import org.jspecify.annotations.NullMarked;

/**
 * 文件存储异常（系统级/不可预期错误，如 IO 失败、非法对象名）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class StorageException extends FrameworkException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
