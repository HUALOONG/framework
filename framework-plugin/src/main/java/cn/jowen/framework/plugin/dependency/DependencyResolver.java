package cn.jowen.framework.plugin.dependency;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件依赖解析器。
 *
 * <p>对依赖图执行 DAG 拓扑排序，检测循环依赖，并拒绝未满足依赖的插件加载请求。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class DependencyResolver {

    private volatile List<String> sortedResult = null;

    /**
     * 解析插件 id 列表的依赖顺序，返回可安全启动的顺序（依赖方在前）。
     *
     * @param descriptors 描述符列表，不可为 {@code null}
     * @return 按启动顺序排列的插件 id 列表
     * @throws DependencyResolutionException 循环依赖或未满足依赖时抛出
     */
    public List<String> resolve(List<cn.jowen.framework.plugin.PluginDescriptor> descriptors)
            throws DependencyResolutionException {
        if (sortedResult != null) {
            return sortedResult;
        }
        Map<String, cn.jowen.framework.plugin.PluginDescriptor> byId = new LinkedHashMap<>();
        for (cn.jowen.framework.plugin.PluginDescriptor d : descriptors) {
            byId.put(d.id(), d);
        }

        // 检查未满足依赖
        for (cn.jowen.framework.plugin.PluginDescriptor d : byId.values()) {
            for (String dep : d.dependencies()) {
                if (!byId.containsKey(dep)) {
                    throw new DependencyResolutionException(
                            "未满足依赖：" + d.id() + " 需要 " + dep + "，但注册表中不存在");
                }
            }
        }

        // Kahn 算法拓扑排序
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (String id : byId.keySet()) {
            inDegree.put(id, 0);
            adj.put(id, new ArrayList<>());
        }
        for (cn.jowen.framework.plugin.PluginDescriptor d : byId.values()) {
            for (String dep : d.dependencies()) {
                adj.get(dep).add(d.id());
                inDegree.merge(d.id(), 1, Integer::sum);
            }
        }

        List<String> queue = new ArrayList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            result.add(current);
            for (String next : adj.get(current)) {
                int deg = inDegree.get(next) - 1;
                inDegree.put(next, deg);
                if (deg == 0) {
                    queue.add(next);
                }
            }
        }

        if (result.size() != byId.size()) {
            throw new DependencyResolutionException("检测到循环依赖，无法完成拓扑排序");
        }

        sortedResult = Collections.unmodifiableList(result);
        return sortedResult;
    }
}
