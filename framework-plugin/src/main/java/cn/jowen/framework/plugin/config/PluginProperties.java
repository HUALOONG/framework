package cn.jowen.framework.plugin.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 插件系统配置属性。对应前缀 {@code framework.plugin}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.plugin")
public final class PluginProperties {

    private boolean enabled = true;
    private String pluginsDir = "plugins";
    private boolean hotSwap = false;
    private PluginClassLoadingProperties classLoading = new PluginClassLoadingProperties();
    private SpringProperties spring = new SpringProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPluginsDir() {
        return pluginsDir;
    }

    public void setPluginsDir(String pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    public boolean isHotSwap() {
        return hotSwap;
    }

    public void setHotSwap(boolean hotSwap) {
        this.hotSwap = hotSwap;
    }

    public PluginClassLoadingProperties getClassLoading() {
        return classLoading;
    }

    public void setClassLoading(PluginClassLoadingProperties classLoading) {
        this.classLoading = classLoading;
    }

    public SpringProperties getSpring() {
        return spring;
    }

    public void setSpring(SpringProperties spring) {
        this.spring = spring;
    }

    /** 类加载策略子属性。 */
    @NullMarked
    @ConfigurationProperties(prefix = "framework.plugin.class-loading")
    public static final class PluginClassLoadingProperties {
        private String strategy = "delegate"; // delegate | isolated
        private java.util.List<String> exportedPackages = java.util.Collections.emptyList();

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public java.util.List<String> getExportedPackages() {
            return exportedPackages;
        }

        public void setExportedPackages(java.util.List<String> exportedPackages) {
            this.exportedPackages = exportedPackages;
        }
    }

    /** Spring 集成子属性。 */
    @NullMarked
    @ConfigurationProperties(prefix = "framework.plugin.spring")
    public static final class SpringProperties {
        private boolean enabled = false;
        private boolean scanComponentScan = true;
        private String basePackage = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isScanComponentScan() {
            return scanComponentScan;
        }

        public void setScanComponentScan(boolean scanComponentScan) {
            this.scanComponentScan = scanComponentScan;
        }

        public String getBasePackage() {
            return basePackage;
        }

        public void setBasePackage(String basePackage) {
            this.basePackage = basePackage;
        }
    }
}
