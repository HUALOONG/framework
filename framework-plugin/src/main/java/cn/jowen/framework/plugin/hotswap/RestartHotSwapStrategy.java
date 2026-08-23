package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.plugin.PluginDescriptor;
import cn.jowen.framework.plugin.PluginLoader;
import org.jspecify.annotations.NullMarked;


/**
 * 重启策略：停止旧插件实例后重新加载同名 jar。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class RestartHotSwapStrategy implements HotSwapStrategy {

    private final cn.jowen.framework.plugin.DefaultPluginManager manager;
    private final java.nio.file.Path pluginsDir;

    public RestartHotSwapStrategy(cn.jowen.framework.plugin.DefaultPluginManager manager, java.nio.file.Path pluginsDir) {
        this.manager = manager;
        this.pluginsDir = pluginsDir;
    }

    @Override
    public void onPluginChange(String fileName) {
        java.nio.file.Path jarPath = pluginsDir.resolve(fileName);
        if (!java.nio.file.Files.isRegularFile(jarPath)) return;
        try {
            PluginDescriptor desc = new cn.jowen.framework.plugin.descriptor.PluginJsonDescriptorParser().parse(jarPath.toString());
            String id = desc.id();
            cn.jowen.framework.plugin.Plugin plugin = manager.get(id);
            if (plugin != null) {
                try { manager.unregister(id); } catch (Exception ignored) {}
            }
            try (PluginLoader loader = new PluginLoader(desc,
                    new java.net.URL[]{jarPath.toUri().toURL()},
                    Thread.currentThread().getContextClassLoader())) {
                manager.load(loader);
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(RestartHotSwapStrategy.class.getName())
                    .warning("插件热部署失败：" + fileName + "，原因：" + e.getMessage());
        }
    }

    /**
     * 关闭指定 ID 的已加载插件并清理类加载器资源。
     *
     * @param id 插件唯一标识，不可为 {@code null}
     */
    public void restart(String id) {
        cn.jowen.framework.plugin.Plugin plugin = manager.get(id);
        if (plugin == null) return;
        try { manager.unregister(id); } catch (Exception ignored) {}
    }
}
