package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 分布式锁 Bean 工厂。
 *
 * <p>负责创建和配置锁相关 Bean，由 boot-autoconfigure 调用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class LockFactory {

    private final LockProperties properties;

    public LockFactory(LockProperties properties) {
        this.properties = properties;
    }

    // TODO: 实现锁 Bean 工厂方法
}
