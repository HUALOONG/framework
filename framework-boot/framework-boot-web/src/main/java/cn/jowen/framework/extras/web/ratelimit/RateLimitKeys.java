package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.core.assertion.Assert;
import org.jspecify.annotations.NullMarked;

/**
 * 限流 Redis key 拼接工具。集中管理命名规范，避免与幂等 / 验证码 / 锁的键空间冲突。
 *
 * <p>格式：{@code ratelimit:{algo}:{dimKey}[:{windowIndex}]}</p>
 * <ul>
 *     <li>{@code ratelimit:} 框架级前缀，可配置（默认 {@code ratelimit:}）；</li>
 *     <li>{@code {algo}} 算法短码 {@code fw} / {@code tb}；</li>
 *     <li>{@code {dimKey}} 业务维度 key（由业务方控制）；</li>
 *     <li>{@code {windowIndex}} 固定窗口的窗口序号，仅固定窗口需要。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
public final class RateLimitKeys {

    /** 默认前缀。 */
    public static final String DEFAULT_PREFIX = "ratelimit:";

    /** 固定窗口算法短码。 */
    private static final String FW = "fw";

    /** 令牌桶算法短码。 */
    private static final String TB = "tb";

    /** 滑动窗口算法短码。 */
    private static final String SW = "sw";

    /** 漏桶算法短码。 */
    private static final String LB = "lb";

    /** prefix 不可变字段。 */
    private final String prefix;

    /**
     * 构造实例（默认前缀）。
     */
    public RateLimitKeys() {
        this(DEFAULT_PREFIX);
    }

    /**
     * 构造实例。
     * @param prefix key 前缀，不可为空白
     */
    public RateLimitKeys(String prefix) {
        Assert.notEmpty(prefix, null, "keyPrefix must not be blank");
        this.prefix = prefix;
    }

    /**
     * 固定窗口 key。
     * @param dimKey 业务维度 key，不可为空白
     * @param windowIndex 窗口序号
     * @return 完整 key
     */
    public String fixedWindow(String dimKey, long windowIndex) {
        Assert.notEmpty(dimKey, null, "dimKey must not be blank");
        return prefix + FW + ':' + dimKey + ':' + windowIndex;
    }

    /**
     * 令牌桶 key。
     * @param dimKey 业务维度 key，不可为空白
     * @return 完整 key
     */
    public String tokenBucket(String dimKey) {
        Assert.notEmpty(dimKey, null, "dimKey must not be blank");
        return prefix + TB + ':' + dimKey;
    }

    /**
     * 滑动窗口 key。
     * @param dimKey 业务维度 key，不可为空白
     * @return 完整 key
     */
    public String slidingWindow(String dimKey) {
        Assert.notEmpty(dimKey, null, "dimKey must not be blank");
        return prefix + SW + ':' + dimKey;
    }

    /**
     * 漏桶 key。
     * @param dimKey 业务维度 key，不可为空白
     * @return 完整 key
     */
    public String leakyBucket(String dimKey) {
        Assert.notEmpty(dimKey, null, "dimKey must not be blank");
        return prefix + LB + ':' + dimKey;
    }

    /**
     * 返回当前前缀。
     * @return 前缀
     */
    public String getPrefix() {
        return prefix;
    }
}
