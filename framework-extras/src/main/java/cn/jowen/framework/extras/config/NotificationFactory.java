package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 消息通知 Bean 工厂。
 *
 * <p>负责创建和配置通知相关 Bean，由 boot-autoconfigure 调用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class NotificationFactory {

    private final NotificationProperties properties;

    public NotificationFactory(NotificationProperties properties) {
        this.properties = properties;
    }

    // TODO: 实现通知 Bean 工厂方法
}
