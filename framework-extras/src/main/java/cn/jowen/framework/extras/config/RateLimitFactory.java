package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 接口限流 Bean 工厂。
 *
 * <p>负责创建和配置限流相关 Bean，由 boot-autoconfigure 调用。
 *
 * @deprecated 由 boot-autoconfigure 装配层直接实例化具体组件取代，本工厂为无 Bean 方法的废弃骨架
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
@Deprecated
public class RateLimitFactory {

    private final RateLimitProperties properties;

    public RateLimitFactory(RateLimitProperties properties) {
        this.properties = properties;
    }

    // TODO: 实现限流 Bean 工厂方法
}
