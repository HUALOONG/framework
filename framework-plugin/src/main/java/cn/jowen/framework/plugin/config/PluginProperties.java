package cn.jowen.framework.plugin.config;

import org.jspecify.annotations.NullMarked;

import java.util.Collections;
import java.util.List;

/**
 * 插件系统配置属性（纯 POJO）。绑定前缀 {@code framework.plugin} 由 boot-autoconfigure 的 {@code @EnableConfigurationProperties} 完成。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginProperties {
    /**
     * 插件系统是否启用。默认启用。
     */
    private boolean enabled = true;

    /**
     * 插件目录。默认当前工作目录下的 {@code ./plugins} 目录。
     */
    private String pluginsDir = "./plugins";

    /**
     * 是否自动启动已加载的插件。默认启用。
     */
    private boolean autoStart = true;

    /**
     * 禁用插件列表。
     */
    private List<String> disabledPlugins = Collections.emptyList();

    /**
     * 热部署子属性。
     */
    private HotSwapProperties hotSwap = new HotSwapProperties();

    /**
     * 类加载策略子属性。
     */
    private ClassLoadingProperties classLoading = new ClassLoadingProperties();

    /**
     * Spring 集成子属性。
     */
    private SpringProperties spring = new SpringProperties();

    /**
     * 健康检查子属性。
     */
    private HealthProperties health = new HealthProperties();

    /**
     * 插件系统是否启用。默认启用。
     *
     * @return 插件系统是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置插件系统是否启用。默认启用。
     *
     * @param enabled 插件系统是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 插件目录。默认当前工作目录下的 {@code ./plugins} 目录。
     *
     * @return 插件目录
     */
    public String getPluginsDir() {
        return pluginsDir;
    }

    /**
     * 设置插件目录。默认当前工作目录下的 {@code ./plugins} 目录。
     *
     * @param pluginsDir 插件目录
     */
    public void setPluginsDir(String pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    /**
     * 是否自动启动已加载的插件。默认启用。
     *
     * @return 是否自动启动已加载的插件
     */
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * 设置是否自动启动已加载的插件。默认启用。
     *
     * @param autoStart 是否自动启动已加载的插件
     */
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    /**
     * 禁用插件列表。
     *
     * @return 禁用插件列表
     */
    public List<String> getDisabledPlugins() {
        return disabledPlugins;
    }

    /**
     * 设置禁用插件列表。
     *
     * @param disabledPlugins 禁用插件列表
     */
    public void setDisabledPlugins(List<String> disabledPlugins) {
        this.disabledPlugins = disabledPlugins;
    }

    /**
     * 热部署子属性。
     *
     * @return 热部署子属性
     */
    public HotSwapProperties getHotSwap() {
        return hotSwap;
    }

    /**
     * 设置热部署子属性。
     *
     * @param hotSwap 热部署子属性
     */
    public void setHotSwap(HotSwapProperties hotSwap) {
        this.hotSwap = hotSwap;
    }

    /**
     * 类加载策略子属性。
     *
     * @return 类加载策略子属性
     */
    public ClassLoadingProperties getClassLoading() {
        return classLoading;
    }

    /**
     * 设置类加载策略子属性。
     *
     * @param classLoading 类加载策略子属性
     */
    public void setClassLoading(ClassLoadingProperties classLoading) {
        this.classLoading = classLoading;
    }

    /**
     * Spring 集成子属性。
     *
     * @return Spring 集成子属性
     */
    public SpringProperties getSpring() {
        return spring;
    }

    /**
     * 设置 Spring 集成子属性。
     *
     * @param spring Spring 集成子属性
     */
    public void setSpring(SpringProperties spring) {
        this.spring = spring;
    }

    /**
     * 健康检查子属性。
     *
     * @return 健康检查子属性
     */
    public HealthProperties getHealth() {
        return health;
    }

    /**
     * 设置健康检查子属性。
     *
     * @param health 健康检查子属性
     */
    public void setHealth(HealthProperties health) {
        this.health = health;
    }

    /**
     * 热部署子属性。
     */
    @NullMarked
    public static final class HotSwapProperties {
        /**
         * 热部署是否启用。默认启用。
         */
        private boolean enabled = true;

        /**
         * 热部署策略。默认重启。
         */
        private String strategy = "restart";

        /**
         * 热部署防抖间隔。默认 3 秒。
         */
        private String debounceInterval = "3s";

        /**
         * 热部署是否启用。默认启用。
         *
         * @return 热部署是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置热部署是否启用。默认启用。
         *
         * @param enabled 热部署是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 热部署策略。默认重启。
         *
         * @return 热部署策略
         */
        public String getStrategy() {
            return strategy;
        }

        /**
         * 设置热部署策略。默认重启。
         *
         * @param strategy 热部署策略
         */
        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        /**
         * 热部署防抖间隔。默认 3 秒。
         *
         * @return 热部署防抖间隔
         */
        public String getDebounceInterval() {
            return debounceInterval;
        }

        /**
         * 设置热部署防抖间隔。默认 3 秒。
         *
         * @param debounceInterval 热部署防抖间隔
         */
        public void setDebounceInterval(String debounceInterval) {
            this.debounceInterval = debounceInterval;
        }
    }

    /**
     * 类加载策略子属性。
     */
    @NullMarked
    public static final class ClassLoadingProperties {
        /**
         * 类加载策略。默认 framework-api-delegate。
         */
        private String strategy = "framework-api-delegate";

        /**
         * 导出包列表。默认 framework-core 和 framework-plugin-api。
         */
        private List<String> exportedPackages = List.of(
                "cn.jowen.framework.core.**",
                "cn.jowen.framework.plugin.api.**"
        );

        /**
         * 隐藏类列表。默认 javax.servlet 包下所有类。
         */
        private List<String> hiddenClasses = List.of("javax.servlet.**");

        /**
         * 共享库列表。默认空。
         */
        private List<String> sharedLibraries = Collections.emptyList();

        /**
         * 类加载策略。默认 framework-api-delegate。
         *
         * @return 类加载策略
         */
        public String getStrategy() {
            return strategy;
        }

        /**
         * 设置类加载策略。默认 framework-api-delegate。
         *
         * @param strategy 类加载策略
         */
        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        /**
         * 导出包列表。默认 framework-core 和 framework-plugin-api。
         *
         * @return 导出包列表
         */
        public List<String> getExportedPackages() {
            return exportedPackages;
        }

        /**
         * 设置导出包列表。默认 framework-core 和 framework-plugin-api。
         *
         * @param exportedPackages 导出包列表
         */
        public void setExportedPackages(List<String> exportedPackages) {
            this.exportedPackages = exportedPackages;
        }

        /**
         * 隐藏类列表。默认 javax.servlet 包下所有类。
         *
         * @return 隐藏类列表
         */
        public List<String> getHiddenClasses() {
            return hiddenClasses;
        }

        /**
         * 设置隐藏类列表。默认 javax.servlet 包下所有类。
         *
         * @param hiddenClasses 隐藏类列表
         */
        public void setHiddenClasses(List<String> hiddenClasses) {
            this.hiddenClasses = hiddenClasses;
        }

        /**
         * 共享库列表。默认空。
         *
         * @return 共享库列表
         */
        public List<String> getSharedLibraries() {
            return sharedLibraries;
        }

        /**
         * 设置共享库列表。默认空。
         *
         * @param sharedLibraries 共享库列表
         */
        public void setSharedLibraries(List<String> sharedLibraries) {
            this.sharedLibraries = sharedLibraries;
        }
    }

    /**
     * Spring 集成子属性。
     */
    @NullMarked
    public static final class SpringProperties {
        /**
         * Spring 集成是否启用。默认启用。
         */
        private boolean enabled = false;

        /**
         * 是否将父上下文中的 Bean 暴露给插件。默认启用。
         */
        private boolean parentContextBeanVisibility = true;

        /**
         * Spring 集成是否启用。默认启用。
         *
         * @return Spring 集成是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置 Spring 集成是否启用。默认启用。
         *
         * @param enabled Spring 集成是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 是否将父上下文中的 Bean 暴露给插件。默认启用。
         *
         * @return 是否将父上下文中的 Bean 暴露给插件
         */
        public boolean isParentContextBeanVisibility() {
            return parentContextBeanVisibility;
        }

        /**
         * 设置是否将父上下文中的 Bean 暴露给插件。默认启用。
         *
         * @param parentContextBeanVisibility 是否将父上下文中的 Bean 暴露给插件
         */
        public void setParentContextBeanVisibility(boolean parentContextBeanVisibility) {
            this.parentContextBeanVisibility = parentContextBeanVisibility;
        }
    }

    /**
     * 健康检查子属性。
     */
    @NullMarked
    public static final class HealthProperties {
        /**
         * 健康检查是否启用。默认启用。
         */
        private boolean enabled = true;

        /**
         * 健康检查端点。默认 /actuator/plugins。
         */
        private String endpoint = "/actuator/plugins";

        /**
         * 健康检查是否启用。默认启用。
         *
         * @return 健康检查是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置健康检查是否启用。默认启用。
         *
         * @param enabled 健康检查是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 健康检查端点。默认 /actuator/plugins。
         *
         * @return 健康检查端点
         */
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * 设置健康检查端点。默认 /actuator/plugins。
         *
         * @param endpoint 健康检查端点
         */
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}
