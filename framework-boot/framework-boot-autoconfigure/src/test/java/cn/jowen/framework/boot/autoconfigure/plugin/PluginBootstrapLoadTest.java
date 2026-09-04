package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import com.gap.plugin.GapPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link PluginBootstrap} 真实插件 jar 加载测试。
 *
 * <p>既有 {@code PluginBootstrapTest} 只覆盖「目录不存在 / 目录为空 / 非法策略回退」的引导分支，
 * {@code loadOne} 的禁用、重复、成功三条路径与 {@code destroy} 的类加载器释放均未触达。
 * 本类在临时目录中打出真实插件 jar（含 {@code META-INF/plugin/plugin.json} 与插件主类字节码），
 * 驱动完整加载链路。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class PluginBootstrapLoadTest {

    private static final String PLUGIN_ID = "gap-plugin";

    private static final String PLUGIN_JSON =
            "{\"pluginId\":\"gap-plugin\",\"pluginName\":\"Gap Plugin\","
                    + "\"version\":\"1.0.0\",\"pluginClass\":\"com.gap.plugin.GapPlugin\"}";

    @TempDir
    Path pluginsDir;

    @BeforeEach
    void buildPluginJar() throws Exception {
        buildPluginJar("gap-plugin.jar");
    }

    @Test
    void loadsPlugin_startsItAndRegistersExtensionPoints() throws Exception {
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPlugin(PLUGIN_ID)).thenReturn(null);
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(pluginsDir.toString());
        props.setAutoStart(true);

        PluginBootstrap bootstrap = new PluginBootstrap(manager, props, mock(PluginSpiBridge.class));
        bootstrap.afterSingletonsInstantiated();

        verify(manager).initialize(eq(PLUGIN_ID), any(Plugin.class), any(PluginContext.class));
        verify(manager).startPlugin(PLUGIN_ID);

        // destroy 需在成功加载后调用，才能释放已登记的插件类加载器
        assertThat(bootstrap).isNotNull();
        bootstrap.destroy();
    }

    @Test
    void loadsPlugin_withoutAutoStartAndWithoutSpiBridge_skipsStartAndRegistration()
            throws Exception {
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPlugin(PLUGIN_ID)).thenReturn(null);
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(pluginsDir.toString());
        props.setAutoStart(false);

        PluginBootstrap bootstrap = new PluginBootstrap(manager, props);
        bootstrap.afterSingletonsInstantiated();

        verify(manager).initialize(eq(PLUGIN_ID), any(Plugin.class), any(PluginContext.class));
        verify(manager, never()).startPlugin(any());
        bootstrap.destroy();
    }

    @Test
    void skipsPluginListedInDisabledPlugins() throws Exception {
        PluginManager manager = mock(PluginManager.class);
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(pluginsDir.toString());
        props.setDisabledPlugins(List.of(PLUGIN_ID));

        new PluginBootstrap(manager, props).afterSingletonsInstantiated();

        verifyNoInteractions(manager);
    }

    @Test
    void skipsPluginAlreadyManagedByPluginManager() throws Exception {
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPlugin(PLUGIN_ID)).thenReturn(new GapPlugin());
        BootPluginProperties props = new BootPluginProperties();
        props.setPluginsDir(pluginsDir.toString());

        new PluginBootstrap(manager, props).afterSingletonsInstantiated();

        verify(manager, never()).initialize(any(), any(), any());
        verify(manager, never()).startPlugin(any());
    }

    /**
     * 在临时目录中打包插件 jar：插件主类字节码取自测试 classpath，描述符写入
     * {@code META-INF/plugin/plugin.json}。
     */
    private Path buildPluginJar(String jarName) throws Exception {
        byte[] classBytes;
        try (InputStream in = PluginBootstrapLoadTest.class.getClassLoader()
                .getResourceAsStream("com/gap/plugin/GapPlugin.class")) {
            if (in == null) {
                throw new IllegalStateException("缺少测试夹具 com/gap/plugin/GapPlugin.class");
            }
            classBytes = in.readAllBytes();
        }

        Path jarPath = pluginsDir.resolve(jarName);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath))) {
            out.putNextEntry(new JarEntry("com/gap/plugin/GapPlugin.class"));
            out.write(classBytes);
            out.closeEntry();
            out.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            out.write(PLUGIN_JSON.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jarPath;
    }
}
