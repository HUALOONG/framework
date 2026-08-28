package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;

/**
 * 限流器抽象，令牌桶语义。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface RateLimiter {

    /**
     * 尝试获取一个令牌。
     *
     * @param key 限流维度 key
     * @return {@code true} 表示放行
     */
    boolean tryAcquire(String key);
}
