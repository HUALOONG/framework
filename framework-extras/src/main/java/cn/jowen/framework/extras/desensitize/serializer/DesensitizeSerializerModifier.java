/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.desensitize.serializer;

import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * 脱敏序列化修改器：在 Jackson 构建序列化器时，将所有 {@code String} 类型替换为
 * {@link DesensitizeJsonSerializer}，从而对任意字符串字段启用（条件式）脱敏。
 *
 * <p>与 {@link DesensitizeModule#DesensitizeModule()} 中的
 * {@code addSerializer(String.class, ...)} 互为冗余保障：即便某条路径未被触发，
 * 另一条仍会把字符串导向脱敏序列化器；最终都由
 * {@link DesensitizeJsonSerializer#createContextual} 按字段注解决定是否脱敏。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class DesensitizeSerializerModifier extends ValueSerializerModifier {

    @Override
    public ValueSerializer<?> modifySerializer(SerializationConfig config,
            BeanDescription.Supplier desc, ValueSerializer<?> serializer) {
        if (String.class.equals(serializer.handledType())) {
            return new DesensitizeJsonSerializer();
        }
        return serializer;
    }
}
