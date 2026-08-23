package cn.jowen.framework.plugin.context;

import cn.jowen.framework.plugin.Plugin;
import cn.jowen.framework.plugin.PluginDescriptor;
import org.jspecify.annotations.NullMarked;

/**
 * 插件抽象基类，内含公共状态字段与模板方法。
 *
 * <p>子类只需实现具体的初始化/启动/停止逻辑，即可复用基础生命周期管理。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public abstract class AbstractPlugin implements Plugin {

    private final PluginDescriptor descriptor;
    private volatile State state = State.CREATED;

    protected AbstractPlugin(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public final String id() {
        return descriptor.id();
    }

    @Override
    public String version() {
        return descriptor.version();
    }

    @Override
    public String description() {
        return descriptor.description();
    }

    public State getState() {
        return state;
    }

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    protected void doInit() throws Exception {}
    protected void doStart() throws Exception {}
    protected void doStop() throws Exception {}

    @Override
    public final void afterPropertiesSet() {
        try {
            doInit();
            state = State.INITIALIZED;
            doStart();
            state = State.STARTED;
        } catch (Exception e) {
            state = State.FAILED;
            throw new RuntimeException("插件启动失败：" + id(), e);
        }
    }

    @Override
    public final void destroy() {
        try {
            doStop();
            state = State.STOPPED;
        } catch (Exception e) {
            state = State.FAILED;
            throw new RuntimeException("插件停止失败：" + id(), e);
        }
    }

    public enum State { CREATED, INITIALIZED, STARTED, STOPPED, FAILED }
}
