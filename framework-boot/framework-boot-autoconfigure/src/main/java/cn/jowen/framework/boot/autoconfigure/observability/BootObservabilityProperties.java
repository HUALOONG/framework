package cn.jowen.framework.boot.autoconfigure.observability;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可观测性配置属性绑定类。
 *
 * <p>仅承载配置，指标装配逻辑由各模块按既有条件接线（cache MeterBinder / jdbc SQL Timer），
 * 本类作为集中总闸的统一配置入口。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.observability")
public class BootObservabilityProperties {

    /**
     * 框架指标总开关，缺省开启。
     */
    private boolean enabled = true;

    /**
     * 框架指标前缀（命名空间），缺省 {@code framework}。
     */
    private String metricsPrefix = "framework";

    /**
     * @return 是否启用框架指标
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用框架指标。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return 指标前缀
     */
    public String getMetricsPrefix() {
        return metricsPrefix;
    }

    /**
     * 设置指标前缀。
     *
     * @param metricsPrefix 指标前缀，不可为空
     */
    public void setMetricsPrefix(String metricsPrefix) {
        this.metricsPrefix = metricsPrefix;
    }
}