package cn.jowen.framework.plugin.qagap;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginJsonDescriptorParser.DescriptorParseException;
import cn.jowen.framework.plugin.descriptor.PluginYamlDescriptorParser;
import cn.jowen.framework.plugin.hotswap.RestartHotSwapStrategy;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import cn.jowen.framework.plugin.loader.FrameworkApiDelegateClassLoader;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 第一梯队缺口补测：覆盖若干当前 FULL_MISSED 行。
 *
 * <ul>
 *   <li>{@code PluginYamlDescriptorParser.load} 缺插件描述符条目时抛异常（行 46）</li>
 *   <li>{@code FrameworkApiDelegateClassLoader.doLoadClass} 父加载器为 null 时回退系统类加载器（行 50）</li>
 *   <li>{@code RestartHotSwapStrategy.unloadPluginByJar} 无匹配映射时走完 for 循环（行 147）</li>
 *   <li>{@code PluginManager.initialize} 默认方法抛 {@link UnsupportedOperationException}（行 48）</li>
 *   <li>{@code PluginLifecycleManager.start} 插件已注册但上下文缺失时抛异常（行 83）</li>
 * </ul>
 */
class PluginFirstTierGapTest {

    @TempDir
    Path tempDir;

    @Test
    void yamlDescriptorParser_loadMissingEntry_throws() throws IOException {
        Path jar = tempDir.resolve("no-yaml.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("readme.txt"));
            jos.write(0);
            jos.closeEntry();
        }
        PluginYamlDescriptorParser parser = new PluginYamlDescriptorParser();
        assertThatThrownBy(() -> parser.load(jar))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void frameworkApiDelegateClassLoader_parentNullFallsBackToSystemClassLoader() {
        FrameworkApiDelegateClassLoader cl =
                new FrameworkApiDelegateClassLoader(new URL[0], null, List.of());
        assertThatThrownBy(() -> cl.loadClass("no.such.Class.To.Exist.xyz"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void restartHotSwapStrategy_onFileDeletedWithoutMapping_reachesLoopEnd() {
        PluginManager mgr = mock(PluginManager.class);
        RestartHotSwapStrategy strategy = new RestartHotSwapStrategy(mgr, tempDir);
        strategy.onFileDeleted("missing-plugin.jar");
        // 无匹配映射，unloadPluginByJar 走完 for 循环后直接返回
        verify(mgr, never()).unloadPlugin(any());
    }

    @Test
    void pluginManager_defaultInitialize_throwsUnsupported() {
        // StubPluginManager 不重写 initialize，直接命中接口的默认实现（行 48）
        PluginManager mgr = new StubPluginManager();
        Plugin plugin = mock(Plugin.class);
        PluginContext ctx = mock(PluginContext.class);
        assertThatThrownBy(() -> mgr.initialize("x", plugin, ctx))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("请使用 PluginLifecycleManager");
    }

    @Test
    void pluginLifecycleManager_startWithoutContext_throws() throws Exception {
        PluginLifecycleManager mgr = new PluginLifecycleManager();
        Field pluginsField = PluginLifecycleManager.class.getDeclaredField("plugins");
        pluginsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Plugin> plugins = (Map<String, Plugin>) pluginsField.get(mgr);
        plugins.put("orphan", mock(Plugin.class));
        // plugins 中存在但 contexts 中缺失 -> start 在行 83 抛 IllegalStateException，
        // 随即被外层 catch 包装为 RuntimeException("插件启动失败：…") 重新抛出
        Throwable thrown = catchThrowable(() -> mgr.start("orphan"));
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("插件启动失败");
        assertThat(thrown.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未初始化");
    }

    /**
     * 仅实现抽象方法、不重写 {@code initialize} 的桩实现，用于触发接口默认方法体。
     */
    static final class StubPluginManager implements PluginManager {
        @Override
        public List<Plugin> loadPlugins(Path pluginsDir) {
            return List.of();
        }

        @Override
        public Plugin loadPlugin(Path pluginPath) {
            return null;
        }

        @Override
        public void unloadPlugin(String pluginId) {
        }

        @Override
        public void startPlugin(String pluginId) {
        }

        @Override
        public void stopPlugin(String pluginId) {
        }

        @Override
        public void restartPlugin(String pluginId) {
        }

        @Override
        public Plugin getPlugin(String pluginId) {
            return null;
        }

        @Override
        public List<Plugin> getPlugins() {
            return List.of();
        }

        @Override
        public List<Plugin> getPluginsByState(PluginState state) {
            return List.of();
        }

        @Override
        public Extension getExtension(String extensionPointId) {
            return null;
        }

        @Override
        public <T> List<T> getExtensions(Class<T> extensionPointClass) {
            return List.of();
        }

        @Override
        public void registerExtensionPoint(ExtensionPoint point) {
        }
    }
}
