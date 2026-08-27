package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import org.jspecify.annotations.NullMarked;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

/**
 * Redis Pub/Sub 订阅监听器：订阅指定频道，收到变更消息即触发目标消息源重载。
 *
 * <p>与 RedisMessageSource 配合实现跨节点消息热更新（发布方在消息变更时 push 频道）。
 * Redisson 为可选依赖。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class RedisSubscriptionWatcher implements ResourceWatcher {

    private final RedissonClient client;
    private final String channel;
    private final ReloadableMessageSource target;
    private final ResourceReloader reloader;

    private volatile boolean running;
    private volatile int listenerId = -1;

    /**
     * 构造订阅监听器。
     *
     * @param client   Redisson 客户端，不可为 {@code null}
     * @param channel  变更通知频道，不可为 {@code null}
     * @param target   待重载消息源，不可为 {@code null}
     * @param reloader 重载执行器，不可为 {@code null}
     */
    public RedisSubscriptionWatcher(RedissonClient client, String channel,
                                    ReloadableMessageSource target, ResourceReloader reloader) {
        this.client = client;
        this.channel = channel;
        this.target = target;
        this.reloader = reloader;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        RTopic topic = client.getTopic(channel);
        listenerId = topic.addListener(String.class,
                (channelName, message) -> reloader.reload(target));
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (listenerId >= 0) {
            client.getTopic(channel).removeListener(listenerId);
        }
        listenerId = -1;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}