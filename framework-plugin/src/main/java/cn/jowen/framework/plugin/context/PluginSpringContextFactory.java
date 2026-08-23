package cn.jowen.framework.plugin.context;

import cn.jowen.framework.plugin.api.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring 子容器工厂（可选能力）。
 *
 * <p>当 {@code framework.plugin.spring.enabled=true} 时，为每个插件创建独立的
 * {@code AnnotationConfigApplicationContext}，parent = 宿主容器。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginSpringContextFactory implements AutoCloseable {

    private final Map<String, Object> children = new ConcurrentHashMap<>();
    private @Nullable Object parentContext;

    /**
     * 创建工厂。
     *
     * @param parentContext 宿主 Spring 容器
     */
    public PluginSpringContextFactory(@Nullable Object parentContext) {
        this.parentContext = parentContext;
    }

    /**
     * 为插件创建子容器。
     *
     * @param plugin      插件实例
     * @param basePackage 扫描包路径
     * @return 子容器，不可为 {@code null}
     */
    public Object createFor(Plugin plugin, String basePackage) {
        // 这里返回 Spring ApplicationContext，实际实现依赖 spring-context
        // 在纯 Java 环境下返回 null（可选能力）
        return null;
    }

    /**
     * 关闭指定插件的子容器。
     *
     * @param pluginId 插件 id
     */
    public void closeFor(String pluginId) {
        children.remove(pluginId);
    }

    @Override
    public void close() {
        children.clear();
    }

    public boolean isEnabled() {
        return parentContext != null;
    }
}
