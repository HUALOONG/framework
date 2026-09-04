package cn.jowen.framework.cache.cache.multilevel;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * 跨节点缓存同步消息，经 Redis pub/sub（Redisson {@code RTopic}）在节点间广播。
 *
 * <p>注意：{@code value} 需可被 Redisson 编解码器序列化；生产环境应保证缓存值为可序列化类型，
 * 并按需配置合适的 {@code Codec}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
public final class CacheSyncMessage implements Serializable {

    /** 事件类型。 */
    public enum Type { PUT, EVICT }

    private final Type type;
    private final String key;
    private final @Nullable Object value;

    public CacheSyncMessage(Type type, String key, @Nullable Object value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public Type type() {
        return type;
    }

    public String key() {
        return key;
    }

    public @Nullable Object value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheSyncMessage that)) return false;
        return type == that.type
                && java.util.Objects.equals(key, that.key)
                && java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, key, value);
    }
}
