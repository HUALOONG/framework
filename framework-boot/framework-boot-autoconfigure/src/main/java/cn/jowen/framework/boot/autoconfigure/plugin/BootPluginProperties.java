package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.config.PluginProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 插件配置属性绑定类。仅在 Boot 装配层标注 {@link ConfigurationProperties}，
 * 避免 framework-plugin 反向依赖 Spring（保持插件核心零 Spring 依赖）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.plugin")
public class BootPluginProperties extends PluginProperties {
}