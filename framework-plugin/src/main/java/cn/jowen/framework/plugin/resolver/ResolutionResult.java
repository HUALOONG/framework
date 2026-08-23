package cn.jowen.framework.plugin.resolver;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 依赖解析结果。
 *
 * @param loadOrder 推荐的加载顺序
 * @param conflicts 发现的冲突列表
 * @param missing   未满足的依赖列表
 * @author 王飞
 */
@NullMarked
public record ResolutionResult(
        List<String> loadOrder,
        List<DependencyConflict> conflicts,
        List<String> missing
) {
    public static ResolutionResult ok(List<String> loadOrder) {
        return new ResolutionResult(loadOrder, List.of(), List.of());
    }

    public static ResolutionResult failed(List<String> missing, List<DependencyConflict> conflicts) {
        return new ResolutionResult(List.of(), conflicts, missing);
    }

    /**
     * 判断依赖是否可解析（无冲突且无缺失）。
     *
     * @return {@code true} 表示可以安全加载
     */
    public boolean isResolvable() {
        return conflicts.isEmpty() && missing.isEmpty();
    }
}
