package cn.jowen.framework.plugin.resolver;

import cn.jowen.framework.plugin.descriptor.PluginDependency;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件依赖解析器。构建 DAG → 拓扑排序 → 版本仲裁 → 返回加载顺序 + 冲突报告。
 *
 * @author 王飞
 */
@NullMarked
public final class DependencyResolver {

    /**
     * 解析插件描述符列表的依赖关系，返回解析结果。
     *
     * @param descriptors 插件描述符列表，不可为 {@code null}
     * @return 解析结果，包含加载顺序、冲突和缺失信息
     */
    public ResolutionResult resolve(List<PluginDescriptor> descriptors) {
        Map<String, PluginDescriptor> byId = new LinkedHashMap<>();
        for (PluginDescriptor d : descriptors) {
            byId.put(d.pluginId(), d);
        }

        // 检查未满足依赖和版本冲突
        List<String> missing = new ArrayList<>();
        List<DependencyConflict> conflicts = new ArrayList<>();

        for (PluginDescriptor d : byId.values()) {
            for (PluginDependency dep : d.requires()) {
                if (!byId.containsKey(dep.pluginId())) {
                    missing.add(d.pluginId() + " -> " + dep.pluginId());
                    continue;
                }
                // 版本冲突检查
                PluginDescriptor depDesc = byId.get(dep.pluginId());
                if (dep.versionRange() != null && !dep.versionRange().contains(depDesc.version())) {
                    conflicts.add(new DependencyConflict(
                            dep.pluginId(), d.pluginId(),
                            List.of(dep.versionRange().toString()), false));
                }
            }
        }

        if (!missing.isEmpty() || !conflicts.isEmpty()) {
            return ResolutionResult.failed(missing, conflicts);
        }

        // 构建依赖图
        DependencyGraph graph = buildGraph(byId);

        // 拓扑排序
        List<String> loadOrder;
        try {
            loadOrder = graph.topologicalSort();
        } catch (DependencyGraph.CycleDetectedException e) {
            return new ResolutionResult(List.of(), List.of(),
                    List.of("检测到循环依赖"));
        }

        return ResolutionResult.ok(loadOrder);
    }

    private DependencyGraph buildGraph(Map<String, PluginDescriptor> byId) {
        DependencyGraph graph = new DependencyGraph();
        for (PluginDescriptor d : byId.values()) {
            graph.addNode(d.pluginId(), d);
        }
        for (PluginDescriptor d : byId.values()) {
            for (PluginDependency dep : d.requires()) {
                graph.addEdge(d.pluginId(), dep.pluginId());
            }
        }
        return graph;
    }
}
