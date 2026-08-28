package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * IP 地域解析 Bean 工厂。
 *
 * <p>负责创建和配置 IP2Region 相关 Bean，由 boot-autoconfigure 调用。
 *
 * @deprecated 由 boot-autoconfigure 装配层直接实例化具体组件取代，本工厂为无 Bean 方法的废弃骨架
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
@Deprecated
public class Ip2RegionFactory {

    private final Ip2RegionProperties properties;

    public Ip2RegionFactory(Ip2RegionProperties properties) {
        this.properties = properties;
    }

    // TODO: 实现 IP2Region Bean 工厂方法
}
