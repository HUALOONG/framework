package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;

/**
 * 限流相关的 Redis Lua 脚本常量（只读、不可变）。
 *
 * <p>脚本统一约定返回 {@code 1} 表示放行、{@code 0} 表示拒绝，
 * 调用方用 {@link cn.jowen.framework.extras.web.lock.RedisScriptReplies#isAllowed(Object)} 收敛判定。
 *
 * <p>本常量中的注释行刻意保留 {@code FIXED_WINDOW} / {@code TOKEN_BUCKET} 标记，
 * 供 {@code FakeRedisCommandExecutor} 按片段包含分派到等价的 Java 实现，使离线测试可真实走通链路。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
public final class RateLimitScripts {

    /** 固定窗口限流脚本。KEYS[1]=计数键；ARGV[1]=permits（窗口内最大放行数）；ARGV[2]=ttlMillis（建议 2×window）。 */
    public static final String FIXED_WINDOW =
            "-- FIXED_WINDOW\n"
            + "local c = redis.call('INCR', KEYS[1])\n"
            + "if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end\n"
            + "if c <= tonumber(ARGV[1]) then return 1 end\n"
            + "return 0\n";

    /** 令牌桶限流脚本。KEYS[1]=桶键；ARGV[1]=capacity；ARGV[2]=windowMillis；ARGV[3]=nowMillis；ARGV[4]=ttlMillis（建议 3×window）。 */
    public static final String TOKEN_BUCKET =
            "-- TOKEN_BUCKET\n"
            + "local data = redis.call('HMGET', KEYS[1], 'tokens', 'ts')\n"
            + "local tokens = tonumber(data[1])\n"
            + "local ts = tonumber(data[2])\n"
            + "local capacity = tonumber(ARGV[1])\n"
            + "local window = tonumber(ARGV[2])\n"
            + "local now = tonumber(ARGV[3])\n"
            + "if tokens == nil or ts == nil then tokens = capacity; ts = now end\n"
            + "if now - ts >= window then tokens = capacity; ts = now end\n"
            + "local allowed = 0\n"
            + "if tokens >= 1 then tokens = tokens - 1; allowed = 1 end\n"
            + "redis.call('HSET', KEYS[1], 'tokens', tostring(tokens), 'ts', tostring(ts))\n"
            + "redis.call('PEXPIRE', KEYS[1], ARGV[4])\n"
            + "return allowed\n";

    /**
     * 滑动窗口限流脚本。KEYS[1]=时间戳有序集合键；
     * ARGV[1]=nowSeconds（当前 unix 时间戳，秒级浮点）；ARGV[2]=windowMillis；
     * ARGV[3]=permits；ARGV[4]=ttlMillis（建议 2×window）。
     *
     * <p>语义：用 ZSET member=unix timestamp 记录每次请求。每次请求时先清除早于
     * {@code now - windowMillis} 的过期 member，再检查 ZCARD 是否达到限额。
     * 返回 1 放行、0 拒绝。
     */
    public static final String SLIDING_WINDOW =
            "-- SLIDING_WINDOW\n"
            + "local now = tonumber(ARGV[1])\n"
            + "local windowSec = tonumber(ARGV[2]) / 1000.0\n"
            + "local permits = tonumber(ARGV[3])\n"
            + "local ttl = tonumber(ARGV[4])\n"
            + "local boundary = now - windowSec\n"
            + "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', boundary)\n"
            + "local count = redis.call('ZCARD', KEYS[1])\n"
            + "local allowed = 0\n"
            + "if count < permits then\n"
            + "    redis.call('ZADD', KEYS[1], now, ngx.now() .. '-' .. count)\n"
            + "    allowed = 1\n"
            + "end\n"
            + "redis.call('PEXPIRE', KEYS[1], ttl)\n"
            + "return allowed\n";

    /**
     * 漏桶限流脚本。KEYS[1]=桶键（存储 "water|last_update_seconds"）；
     * ARGV[1]=nowSeconds；ARGV[2]=leakPerSecond（每秒漏水份额，=capacity/windowSeconds）；
     * ARGV[3]=capacity。
     *
     * <p>语义：先按经过时间计算漏水量，再判断当前水位 + 1 是否超容。
     * 返回 1 放行、0 拒绝。
     */
    public static final String LEAKY_BUCKET =
            "-- LEAKY_BUCKET\n"
            + "local now = tonumber(ARGV[1])\n"
            + "local leakPerSec = tonumber(ARGV[2])\n"
            + "local capacity = tonumber(ARGV[3])\n"
            + "local data = redis.call('HMGET', KEYS[1], 'water', 'last')\n"
            + "local water = tonumber(data[1]) or 0.0\n"
            + "local last = tonumber(data[2]) or now\n"
            + "local elapsed = now - last\n"
            + "if elapsed > 0 then\n"
            + "    water = math.max(0, water - elapsed * leakPerSec)\n"
            + "    last = now\n"
            + "end\n"
            + "local allowed = 0\n"
            + "if water + 1 <= capacity then\n"
            + "    water = water + 1\n"
            + "    allowed = 1\n"
            + "end\n"
            + "redis.call('HSET', KEYS[1], 'water', tostring(water), 'last', tostring(last))\n"
            + "return allowed\n";

    private RateLimitScripts() {
    }
}
