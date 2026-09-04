package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;
import org.redisson.api.RTopic;

/**
 * 基于 Redisson {@code RTopic} 的跨节点缓存同步实现（底层即 Redis pub/sub）。
 *
 * <ul>
 *   <li>接收侧：订阅主题，远端 {@link CacheSyncMessage} 到达后调用 {@link #onCachePut}/{@link #onCacheEvict}
 *       直接更新<b>本地</b>多级缓存（不经过装饰器，避免回环重广播）；</li>
 *   <li>发布侧：通过 {@link #publishPut}/{@link #publishEvict} 将本地写/失效广播给其余节点。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class RedissonCacheSyncListener implements CacheSyncListener, CacheSyncBroadcaster {

    private final Cache<Object, Object> localCache;
    private final RTopic topic;

    /**
     * 构造并立即订阅同步主题。
     *
     * @param localCache 本地多级缓存（远端事件将直接写入此处），不可为 {@code null}
     * @param client     Redisson 客户端，不可为 {@code null}
     * @param topicName  同步主题名，不可为 {@code null}
     */
    public RedissonCacheSyncListener(Cache<Object, Object> localCache, RedissonClient client, String topicName) {
        this.localCache = localCache;
        this.topic = client.getTopic(topicName);
        this.topic.addListener(CacheSyncMessage.class, (channel, msg) -> apply(msg));
    }

    private void apply(CacheSyncMessage msg) {
        if (msg.type() == CacheSyncMessage.Type.PUT) {
            onCachePut(msg.key(), msg.value());
        } else {
            onCacheEvict(msg.key());
        }
    }

    @Override
    public void onCachePut(String key, @Nullable Object value) {
        localCache.put(key, value);
    }

    @Override
    public void onCacheEvict(String key) {
        localCache.evict(key);
    }

    @Override
    public void publishPut(String key, @Nullable Object value) {
        topic.publish(new CacheSyncMessage(CacheSyncMessage.Type.PUT, key, value));
    }

    @Override
    public void publishEvict(String key) {
        topic.publish(new CacheSyncMessage(CacheSyncMessage.Type.EVICT, key, null));
    }
}
