/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.strategy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 日期路径命名策略：生成 {@code yyyy/MM/dd/<uuid>_<原名>} 形式对象名。
 *
 * <p>默认基于当前日期；可注入 {@link DateTimeFormatter} 与日期供应器以便测试。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class DatePathStrategy implements ObjectNameStrategy {

    private final DateTimeFormatter formatter;
    private final Supplier<LocalDate> dateSupplier;

    public DatePathStrategy() {
        this(DateTimeFormatter.ofPattern("yyyy/MM/dd"), LocalDate::now);
    }

    public DatePathStrategy(DateTimeFormatter formatter, Supplier<LocalDate> dateSupplier) {
        this.formatter = formatter == null ? DateTimeFormatter.ofPattern("yyyy/MM/dd") : formatter;
        this.dateSupplier = dateSupplier == null ? LocalDate::now : dateSupplier;
    }

    @Override
    public String generate(@Nullable String originalFilename) {
        String base = basename(originalFilename);
        String date = formatter.format(dateSupplier.get());
        return date + "/" + UUID.randomUUID().toString().replace("-", "") + "_" + base;
    }

    static String basename(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return "file";
        }
        int idx = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return idx >= 0 ? name.substring(idx + 1) : name;
    }
}
