package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
public class PluginAutoConfiguration {

    /**
     * 插件生命周期管理器：状态机驱动插件的 initialize/start/stop/restart/destroy。
     */
    @Bean
    @ConditionalOnMissingBean(PluginManager.class)
    public PluginLifecycleManager pluginLifecycleManager() {
        return new PluginLifecycleManager();
    }
}