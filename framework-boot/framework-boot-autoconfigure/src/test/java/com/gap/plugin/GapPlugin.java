package com.gap.plugin;

import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.support.AbstractPlugin;

/**
 * 插件引导测试夹具：可被打包进 jar 供 {@code PluginLoader} 反射实例化。
 *
 * <p>包名刻意落在 {@code cn.jowen.framework} 之外，确保该类只能由插件类加载器从 jar 中解析，
 * 而非经框架 API 委派路径命中测试 classpath。
 */
public class GapPlugin extends AbstractPlugin {

    public GapPlugin() {
        super(new PluginDescriptor("gap-plugin"));
    }

    @Override
    protected void doStart(PluginContext context) {
    }

    @Override
    protected void doStop() {
    }
}
