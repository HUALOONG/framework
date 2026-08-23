package cn.jowen.framework.plugin.support;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 插件抽象基类，内含公共状态字段与模板方法。
 *
 * <p>子类只需实现 {@link #doStart(PluginContext)} / {@link #doStop()} 即可复用基础生命周期。
 *
 * @author 王飞
 */
@NullMarked
public abstract class AbstractPlugin implements Plugin {

    private final PluginDescriptor descriptor;
    private volatile PluginState state = PluginState.CREATED;
    private @Nullable PluginContext context;

    protected AbstractPlugin(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public final PluginDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public final PluginState getState() {
        return state;
    }

    /**
     * 返回注入的运行时上下文。
     */
    @Nullable
    public PluginContext getContext() {
        return context;
    }

    /**
     * 注入运行时上下文。由 {@link cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager} 在注册时调用。
     */
    public void setContext(@Nullable PluginContext ctx) {
        this.context = ctx;
    }

    protected void doInit() throws Exception {
    }

    protected abstract void doStart(PluginContext context) throws Exception;

    protected abstract void doStop() throws Exception;

    @Override
    public final void start(PluginContext context) {
        try {
            doInit();
            state = PluginState.STARTING;
            doStart(context);
            state = PluginState.STARTED;
        } catch (Exception e) {
            state = PluginState.FAILED;
            throw new RuntimeException("插件启动失败：" + descriptor.pluginId(), e);
        }
    }

    @Override
    public final void stop() {
        try {
            doStop();
            state = PluginState.STOPPED;
        } catch (Exception e) {
            state = PluginState.FAILED;
            throw new RuntimeException("插件停止失败：" + descriptor.pluginId(), e);
        }
    }
}
