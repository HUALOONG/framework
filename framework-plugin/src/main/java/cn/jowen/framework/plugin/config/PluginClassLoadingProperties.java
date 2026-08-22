package cn.jowen.framework.plugin.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 插件类加载策略配置。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.plugin.class-loading")
public final class PluginClassLoadingProperties {

    private String strategy = "delegate";
    private java.util.List<String> exportedPackages = java.util.Collections.emptyList();

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public java.util.List<String> getExportedPackages() { return exportedPackages; }
    public void setExportedPackages(java.util.List<String> exportedPackages) { this.exportedPackages = exportedPackages; }
}
