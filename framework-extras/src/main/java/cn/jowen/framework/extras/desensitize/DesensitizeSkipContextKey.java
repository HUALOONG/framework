/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.desensitize;

import cn.jowen.framework.core.context.ContextKey;
import org.jspecify.annotations.NullMarked;

/**
 * 脱敏跳过开关上下文键。
 *
 * <p>在序列化期间（如管理员查看明细）将本键设为 {@code true}，
 * {@link cn.jowen.framework.extras.desensitize.serializer.DesensitizeJsonSerializer}
 * 会跳过全部字段脱敏，原样输出明文；作用域结束（{@code runWith} 退出）自动恢复。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class DesensitizeSkipContextKey {

    /** 全局跳过脱敏开关：值为 {@code true} 时序列化不脱敏。 */
    public static final ContextKey<Boolean> DESENSITIZE_SKIP =
            ContextKey.named("desensitize.skip", Boolean.class);

    private DesensitizeSkipContextKey() {
    }
}
