/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.desensitize;

import org.jspecify.annotations.NullMarked;

/**
 * 脱敏能力配置项（纯 POJO，不含 {@code @ConfigurationProperties}，
 * 由启动层/自动配置在装配时按需绑定，见架构设计 12 条约定）。
 *
 * <p>当前仅暴露总开关；序列化期是否免脱敏（管理员场景）由
 * {@link DesensitizeSkipContextKey#DESENSITIZE_SKIP} 在作用域内动态控制。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class DesensitizeProperties {

    /** 是否启用脱敏能力，默认 true。 */
    private boolean enabled = true;

    /** 未知脱敏策略是否抛异常（false 时按原值返回），默认 false。 */
    private boolean failOnUnknownStrategy = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailOnUnknownStrategy() {
        return failOnUnknownStrategy;
    }

    public void setFailOnUnknownStrategy(boolean failOnUnknownStrategy) {
        this.failOnUnknownStrategy = failOnUnknownStrategy;
    }
}
