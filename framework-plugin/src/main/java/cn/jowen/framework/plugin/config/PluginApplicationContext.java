package cn.jowen.framework.plugin.config;

import cn.jowen.framework.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件 Spring 子容器持有器。在 {@code spring.enabled=true} 时为每个插件创建独立的
 * {@link AnnotationConfigApplicationContext}，parent = 宿主容器，插件可访问宿主 Bean。
 *
 * <p>生命周期：{@link #createFor(Plugin)} 创建 → 插件运行期间可用 →
 * {@link #closeFor(String)} 或 {@link #close()} 关闭，释放所有 Bean 和资源。
 *
 * @author Jowen
 */
@NullMarked
public final class PluginApplicationContext implements AutoCloseable, ApplicationContextAware {

    private ApplicationContext parent;
    private String basePackage = "";
    private boolean scanComponentScan = true;
    private final Map<String, AnnotationConfigApplicationContext> children = new ConcurrentHashMap<>();

    private PluginApplicationContext() {}

    /**
     * 创建并配置一个新的 {@link PluginApplicationContext} 实例。
     *
     * @param parent 宿主 Spring 容器，不可为 {@code null}
     * @return 新实例，不可为 {@code null}
     */
    public static PluginApplicationContext create(ApplicationContext parent) {
        PluginApplicationContext ctx = new PluginApplicationContext();
        ctx.parent = parent;
        return ctx;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.parent = applicationContext;
    }

    /**
     * 配置扫描参数。必须在 {@link #createFor} 之前调用。
     *
     * @param basePackage       扫描基础包，{@code null} 或空字符串表示不扫描组件
     * @param scanComponentScan 是否启用 {@code @ComponentScan}，默认 true
     */
    public void configure(String basePackage, boolean scanComponentScan) {
        this.basePackage = basePackage == null ? "" : basePackage;
        this.scanComponentScan = scanComponentScan;
    }

    /**
     * 为插件创建子容器，将插件实例注册为 Bean。
     *
     * @param plugin 插件实例，不可为 {@code null}
     * @return 创建的子容器，不可为 {@code null}
     */
    public AnnotationConfigApplicationContext createFor(Plugin plugin) {
        AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
        child.setParent(parent);
        child.registerBean(Plugin.class, () -> plugin);
        if (scanComponentScan && !basePackage.isEmpty()) {
            child.scan(basePackage);
        }
        child.refresh();
        children.put(plugin.id(), child);
        return child;
    }

    /**
     * 关闭并销毁指定插件的子容器。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     */
    public void closeFor(String pluginId) {
        AnnotationConfigApplicationContext child = children.remove(pluginId);
        if (child != null) {
            try {
                child.close();
            } catch (Exception ignored) {
                // 忽略关闭时的异常
            }
        }
    }

    @Override
    public void close() {
        for (AnnotationConfigApplicationContext child : children.values()) {
            try {
                child.close();
            } catch (Exception ignored) {
                // 忽略关闭时的异常
            }
        }
        children.clear();
    }

    /**
     * 返回是否为给定插件创建了子容器。
     *
     * @param pluginId 插件 id
     * @return 是否存在子容器
     */
    public boolean hasChild(String pluginId) {
        return children.containsKey(pluginId);
    }
}
