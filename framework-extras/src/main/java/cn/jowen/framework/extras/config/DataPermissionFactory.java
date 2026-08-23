package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 数据权限 Bean 工厂。
 *
 * <p>负责创建和配置数据权限相关 Bean，由 boot-autoconfigure 调用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class DataPermissionFactory {

    private final DataPermissionProperties properties;

    public DataPermissionFactory(DataPermissionProperties properties) {
        this.properties = properties;
    }

    // TODO: 实现数据权限 Bean 工厂方法
}
