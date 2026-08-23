package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.plugin.DefaultPluginManager;
import cn.jowen.framework.plugin.Plugin;
import cn.jowen.framework.plugin.PluginManager;
import cn.jowen.framework.plugin.classloader.PluginClassLoaderConfig;
import cn.jowen.framework.plugin.config.PluginApplicationContext;
import cn.jowen.framework.plugin.config.PluginProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 插件装配。向容器提供 {@link PluginManager}，并自动注册 classpath 中带 {@code @Activate} 的插件实现
 * （基于 core 的 SPI 机制），满足"插件热部署/自动发现"能力。隔离类加载的热加载由 {@link PluginManager#load} 提供。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@AutoConfiguration(after = BootAutoConfiguration.class)
@ConditionalOnClass(name = "cn.jowen.framework.plugin.PluginManager")
@ConditionalOnProperty(prefix = "framework.plugin", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(PluginProperties.class)
public class PluginAutoConfiguration {

    /**
     * 插件管理器。创建时自动注册所有激活插件。
     *
     * @param props 插件配置，用于把类加载策略/导出包写入 {@link PluginClassLoaderConfig}
     * @param springContext Spring 子容器持有器，可为 {@code null}（未启用时）
     * @return 插件管理器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginManager pluginManager(PluginProperties props,
                                       @Nullable PluginApplicationContext springContext) {
        PluginClassLoaderConfig.configure(
                props.getClassLoading().getStrategy(),
                props.getClassLoading().getExportedPackages());
        DefaultPluginManager manager = new DefaultPluginManager();
        manager.setSpringContext(springContext);
        for (Plugin plugin : ExtensionLoader.getExtensionLoader(Plugin.class).getActivateExtensions()) {
            manager.register(plugin);
        }
        return manager;
    }

    /**
     * Spring 子容器持有器。仅在 {@code framework.plugin.spring.enabled=true} 时创建。
     *
     * @param applicationContext 宿主 Spring 容器
     * @param props 插件配置，读取 spring 子属性
     * @return 子容器持有器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnProperty(prefix = "framework.plugin.spring", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public PluginApplicationContext pluginApplicationContext(
            ApplicationContext applicationContext, PluginProperties props) {
        PluginApplicationContext ctx = PluginApplicationContext.create(applicationContext);
        PluginProperties.SpringProperties spring = props.getSpring();
        ctx.configure(spring.getBasePackage(), spring.isScanComponentScan());
        return ctx;
    }
}
