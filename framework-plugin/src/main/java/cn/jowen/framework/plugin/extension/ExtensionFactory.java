package cn.jowen.framework.plugin.extension;

import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.jspecify.annotations.NullMarked;

/**
 * 扩展实例化工厂。负责 ClassLoader 加载 → 构造器注入 PluginContext → 配置注入。
 *
 * @author 王飞
 */
@NullMarked
public final class ExtensionFactory {

    private final ExtensionRegistry registry;
    private final PluginContext context;

    public ExtensionFactory(ExtensionRegistry registry, PluginContext context) {
        this.registry = registry;
        this.context = context;
    }

    /**
     * 从插件类加载器加载并注册扩展。
     *
     * @param classLoader 插件类加载器
     * @param basePackage 扫描包路径
     */
    public void loadFromPackage(ClassLoader classLoader, String basePackage) {
        ExtensionScanner scanner = new ExtensionScanner(registry);
        scanner.scanAndRegister(basePackage);
    }

    /**
     * 从扩展定义列表加载并注册。
     *
     * @param definitions 扩展定义列表
     * @param classLoader 类加载器
     */
    public void loadFromDefinitions(java.util.List<ExtensionDefinition> definitions, ClassLoader classLoader) {
        ExtensionScanner scanner = new ExtensionScanner(registry);
        scanner.loadFromDescriptor(definitions, classLoader);
    }

    /**
     * 获取指定扩展点的所有扩展实现。
     *
     * @param extensionPointId 扩展点 id
     * @param <T>              扩展点类型
     * @return 扩展列表
     */
    @SuppressWarnings("unchecked")
    public <T> java.util.List<T> getExtensions(String extensionPointId) {
        return (java.util.List<T>) (java.util.List<?>) registry.getExtensions(extensionPointId);
    }

    public ExtensionRegistry getRegistry() {
        return registry;
    }
}
