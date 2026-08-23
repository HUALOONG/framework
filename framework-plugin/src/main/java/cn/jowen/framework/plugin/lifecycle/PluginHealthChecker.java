package cn.jowen.framework.plugin.lifecycle;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginState;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 插件健康检查器。检查插件状态、依赖满足情况和自定义健康检查。
 *
 * @author 王飞
 */
@NullMarked
public interface PluginHealthChecker {

    /**
     * 获取所有健康检查器的列表。
     */
    static List<PluginHealthChecker> of() {
        return List.of();
    }

    /**
     * 检查插件健康状态。
     *
     * @param plugin 插件实例
     * @return 健康检查结果
     */
    HealthResult check(Plugin plugin);

    /**
     * 健康检查结果。
     *
     * @param healthy 是否健康
     * @param message 健康说明
     * @param state   当前状态
     */
    record HealthResult(boolean healthy, String message, PluginState state) {
        public static HealthResult ok(PluginState state) {
            return new HealthResult(true, "healthy", state);
        }

        public static HealthResult fail(PluginState state, String reason) {
            return new HealthResult(false, reason, state);
        }
    }
}
