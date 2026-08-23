package pluginimpl;

import cn.jowen.framework.plugin.Plugin;

/**
 * 加载器测试用桩插件。刻意置于非 {@code cn.jowen.framework} 包下，使 delegate 策略不会将其委派给父加载器，
 * 从而能被插件内部分类加载器（{@link cn.jowen.framework.plugin.classloader.PluginClassLoader}）自身定义，
 * 用以验证「插件私有类由自身类加载器解析、框架类委派父共享」。
 */
public final class StubPlugin implements Plugin {

    public StubPlugin() {
        // 反射实例化所需的无参构造器
    }

    @Override
    public String id() {
        return "stub";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public void afterPropertiesSet() {
        // 无需初始化
    }

    @Override
    public void destroy() {
        // 无需清理
    }
}
