package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 接口限流配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class RateLimitProperties {

    /** enabled 字段。 */
    private boolean enabled = true;
    /** defaultAlgorithm 字段。 */
    private RateLimitAlgorithm defaultAlgorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * 获取enabled。
     * @return 结果
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置enabled。
     * @param enabled 参数 enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取default algorithm。
     * @return 结果
     */
    public RateLimitAlgorithm getDefaultAlgorithm() {
        return defaultAlgorithm;
    }

    /**
     * 设置default algorithm。
     * @param defaultAlgorithm 参数 defaultAlgorithm
     */
    public void setDefaultAlgorithm(RateLimitAlgorithm defaultAlgorithm) {
        this.defaultAlgorithm = defaultAlgorithm;
    }
}
