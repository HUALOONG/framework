package cn.jowen.framework.extras.web.idempotent;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;

/**
 * 幂等指纹存储抽象。建议以 Redis 实现以支持分布式场景。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface IdempotentStore {

    /**
     * 尝试记录指纹。仅当指纹不存在时写入成功。
     *
     * @param key    幂等 key
     * @param expire 过期时间
     * @param unit   时间单位
     * @return {@code true} 表示首次写入（放行），{@code false} 表示已存在（拒绝）
     */
    boolean tryMark(String key, long expire, TimeUnit unit);

    /**
     * 移除指纹。
     *
     * @param key 幂等 key
     */
    void remove(String key);
}
