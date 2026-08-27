package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import cn.jowen.framework.plugin.resolver.DependencyResolver;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 插件能力装配。当 classpath 存在 {@code cn.jowen.framework.plugin.api.PluginManager} 且
 * {@code framework.plugin.enabled=true}（缺省即开）时，注册 {@link PluginLifecycleManager}。
 *
 * <p>业务方可通过 {@code @Autowired PluginManager} 注入后动态加载/启动/停止插件。
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@ConditionalOnClass(name = "cn.jowen.framework.plugin.api.PluginManager")
@ConditionalOnProperty(prefix = "framework.plugin", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(BootPluginProperties.class)
public class PluginAutoConfiguration {

    /**
     * 插件生命周期管理器：状态机驱动插件的 initialize/start/stop/restart/destroy，扩展查询委托 {@link ExtensionRegistry}。
     *
     * @param extensionRegistry 扩展注册中心（可选，{@code null} 时扩展查询返回空）
     * @return 生命周期管理器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean(PluginManager.class)
    public PluginLifecycleManager pluginLifecycleManager(ObjectProvider<ExtensionRegistry> extensionRegistry) {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        ExtensionRegistry registry = extensionRegistry.getIfAvailable();
        if (registry != null) {
            manager.setExtensionRegistry(registry);
        }
        return manager;
    }

    /**
     * 扩展注册中心：插件扩展实例的注册与按扩展点/类型查询。
     *
     * @return 扩展注册中心，不可为 {@code null}
     */
    @Bean("pluginExtensionRegistry")
    @ConditionalOnMissingBean
    public ExtensionRegistry pluginExtensionRegistry() {
        return new ExtensionRegistry();
    }

    /**
     * 依赖解析器：批量加载时按 DAG 拓扑排序、版本仲裁与循环检测。
     *
     * @return 依赖解析器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public DependencyResolver dependencyResolver() {
        return new DependencyResolver();
    }

    /**
     * 插件自动加载引导器：容器就绪后扫描 plugins-dir 加载并按 autoStart 启动，关闭时释放类加载器。
     *
     * @param pluginManager 插件管理器，不可为 {@code null}
     * @param properties    插件配置，不可为 {@code null}
     * @return 引导器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginBootstrap pluginBootstrap(PluginManager pluginManager, BootPluginProperties properties) {
        return new PluginBootstrap(pluginManager, properties);
    }

    /**
     * Actuator 插件管理端点（/actuator/plugins），仅当 actuator 在 classpath 时生效。
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    public PluginEndpoint pluginEndpoint(PluginManager pluginManager) {
        return new PluginEndpoint(pluginManager);
    }

    /**
     * 插件健康指示器，仅当 spring-boot-health 在 classpath 时生效。
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    public PluginHealthIndicator pluginHealthIndicator(PluginManager pluginManager) {
        return new PluginHealthIndicator(pluginManager);
    }
}