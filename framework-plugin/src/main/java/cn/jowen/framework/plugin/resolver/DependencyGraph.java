package cn.jowen.framework.plugin.resolver;

import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 插件依赖图。支持添加节点/边、拓扑排序、循环检测和传递依赖获取。
 *
 * @author 王飞
 */
@NullMarked
public final class DependencyGraph {

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, List<String>> adj = new HashMap<>();
    private final Map<String, Integer> inDegree = new HashMap<>();

    /**
     * 添加节点。
     */
    public void addNode(String id, PluginDescriptor descriptor) {
        if (!nodes.containsKey(id)) {
            nodes.put(id, new Node(id, descriptor));
            adj.put(id, new ArrayList<>());
            inDegree.put(id, 0);
        }
    }

    /**
     * 添加有向边：from 依赖 to（to 必须先于 from 加载）。
     */
    public void addEdge(String from, String to) {
        adj.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        inDegree.merge(to, 1, Integer::sum);
    }

    /**
     * 拓扑排序（Kahn 算法）。
     *
     * @return 排序后的节点 id 列表（依赖方在前）
     * @throws CycleDetectedException 存在循环依赖时抛出
     */
    public List<String> topologicalSort() {
        Map<String, Integer> degree = new HashMap<>(inDegree);
        List<String> queue = new ArrayList<>();
        for (Map.Entry<String, Integer> e : degree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }
        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            result.add(current);
            for (String neighbor : adj.getOrDefault(current, List.of())) {
                int deg = degree.get(neighbor) - 1;
                degree.put(neighbor, deg);
                if (deg == 0) queue.add(neighbor);
            }
        }
        if (result.size() != nodes.size()) {
            throw new CycleDetectedException("检测到循环依赖，无法完成拓扑排序");
        }
        return List.copyOf(result);
    }

    /**
     * 检测循环依赖。
     *
     * @return {@code true} 存在循环
     */
    public boolean detectCycles() {
        try {
            topologicalSort();
            return false;
        } catch (CycleDetectedException e) {
            return true;
        }
    }

    /**
     * 获取指定节点的传递依赖（所有间接依赖的 id 集合）。
     *
     * @param nodeId 节点 id
     * @return 传递依赖 id 集合
     */
    public List<String> getTransitiveDependencies(String nodeId) {
        Set<String> result = new java.util.LinkedHashSet<>();
        dfs(nodeId, result);
        return List.copyOf(result);
    }

    private void dfs(String nodeId, Set<String> visited) {
        for (String dep : adj.getOrDefault(nodeId, List.of())) {
            if (visited.add(dep)) {
                dfs(dep, visited);
            }
        }
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    /**
     * 依赖图节点。
     */
    public static final class Node {
        public final String id;
        public final PluginDescriptor descriptor;

        public Node(String id, PluginDescriptor descriptor) {
            this.id = id;
            this.descriptor = descriptor;
        }
    }

    /**
     * 循环依赖异常。
     */
    public static final class CycleDetectedException extends RuntimeException {
        public CycleDetectedException(String message) {
            super(message);
        }
    }
}
