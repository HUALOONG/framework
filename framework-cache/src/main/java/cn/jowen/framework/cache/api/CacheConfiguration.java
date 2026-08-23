package cn.jowen.framework.cache.api;

import cn.jowen.framework.cache.eviction.EvictionPolicy;
import cn.jowen.framework.cache.serializer.CacheSerializer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * 缓存配置。用于构建 {@link Cache} 实例的完整配置参数，支持链式构造。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class CacheConfiguration {

    /**
     * 最大容量；{@code <=0} 表示无上限。
     */
    private final long maxSize;
    /**
     * 写入后过期时间；{@code null} 表示不过期。
     */
    @Nullable
    private final Duration expireAfterWrite;
    /**
     * 访问后过期时间；{@code null} 表示不使用。
     */
    @Nullable
    private final Duration expireAfterAccess;
    /**
     * 序列化器；{@code null} 表示使用默认序列化。
     */
    @Nullable
    private final CacheSerializer serializer;
    /**
     * 淘汰策略；{@code null} 表示使用默认策略（LRU）。
     */
    @Nullable
    private final EvictionPolicy evictionPolicy;
    /**
     * 是否缓存空值（防穿透）；默认 false。
     */
    private final boolean nullValueCache;
    /**
     * 过期时间抖动（防雪崩）；{@code null} 表示不抖动。
     */
    @Nullable
    private final Duration jitter;
    /**
     * 互斥锁（防击穿）；默认 false。
     */
    private final boolean mutexLock;

    private CacheConfiguration(Builder builder) {
        this.maxSize = builder.maxSize;
        this.expireAfterWrite = builder.expireAfterWrite;
        this.expireAfterAccess = builder.expireAfterAccess;
        this.serializer = builder.serializer;
        this.evictionPolicy = builder.evictionPolicy;
        this.nullValueCache = builder.nullValueCache;
        this.jitter = builder.jitter;
        this.mutexLock = builder.mutexLock;
    }

    /**
     * @return 新的 Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    public long maxSize() {
        return maxSize;
    }

    @Nullable
    public Duration expireAfterWrite() {
        return expireAfterWrite;
    }

    @Nullable
    public Duration expireAfterAccess() {
        return expireAfterAccess;
    }

    @Nullable
    public CacheSerializer serializer() {
        return serializer;
    }

    @Nullable
    public EvictionPolicy evictionPolicy() {
        return evictionPolicy;
    }

    public boolean isNullValueCache() {
        return nullValueCache;
    }

    @Nullable
    public Duration jitter() {
        return jitter;
    }

    public boolean isMutexLock() {
        return mutexLock;
    }

    /**
     * 缓存配置构建器。
     */
    @NullMarked
    public static final class Builder {
        private long maxSize = 0L;
        @Nullable
        private Duration expireAfterWrite = null;
        @Nullable
        private Duration expireAfterAccess = null;
        @Nullable
        private CacheSerializer serializer = null;
        @Nullable
        private EvictionPolicy evictionPolicy = null;
        private boolean nullValueCache = false;
        @Nullable
        private Duration jitter = null;
        private boolean mutexLock = false;

        private Builder() {
        }

        public Builder maxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder expireAfterWrite(Duration duration) {
            this.expireAfterWrite = duration;
            return this;
        }

        public Builder expireAfterAccess(Duration duration) {
            this.expireAfterAccess = duration;
            return this;
        }

        public Builder serializer(CacheSerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder evictionPolicy(EvictionPolicy policy) {
            this.evictionPolicy = policy;
            return this;
        }

        public Builder nullValueCache(boolean enabled) {
            this.nullValueCache = enabled;
            return this;
        }

        public Builder jitter(Duration duration) {
            this.jitter = duration;
            return this;
        }

        public Builder mutexLock(boolean enabled) {
            this.mutexLock = enabled;
            return this;
        }

        public CacheConfiguration build() {
            return new CacheConfiguration(this);
        }
    }
}
