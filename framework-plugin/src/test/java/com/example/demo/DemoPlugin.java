package com.example.demo;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;

/**
 * 热部署测试用示例插件（打包进临时插件 jar，经插件类加载器隔离加载）。
 */
public class DemoPlugin implements Plugin {

    private PluginState state = PluginState.CREATED;

    @Override
    public void start(PluginContext context) {
        this.state = PluginState.STARTED;
    }

    @Override
    public void stop() {
        this.state = PluginState.STOPPED;
    }

    @Override
    public PluginDescriptor getDescriptor() {
        return PluginDescriptor.of("demo", "1.0.0", "com.example.demo.DemoPlugin");
    }

    @Override
    public PluginState getState() {
        return state;
    }
}