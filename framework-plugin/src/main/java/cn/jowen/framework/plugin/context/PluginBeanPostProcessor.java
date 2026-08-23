package cn.jowen.framework.plugin.context;

import cn.jowen.framework.plugin.api.PluginContext;
import org.jspecify.annotations.NullMarked;

/**
 * Bean 后置处理器，自动注入 PluginContext / PluginConfiguration / SharedData。
 *
 * <p>仅在启用 Spring 子容器时有效。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginBeanPostProcessor {

    /**
     * 在 Bean 初始化前注入插件上下文。
     *
     * @param bean     Bean 实例
     * @param beanName Bean 名称
     * @param context  插件上下文
     */
    public void postProcessBeforeInitialization(Object bean, String beanName, PluginContext context) {
        if (bean instanceof PluginContextAware aware) {
            aware.setPluginContext(context);
        }
    }

    /**
     * 在 Bean 初始化后注入插件上下文。
     *
     * @param bean    Bean 实例
     * @param context 插件上下文
     */
    public void postProcessAfterInitialization(Object bean, PluginContext context) {
        if (bean instanceof PluginConfigurationTarget configTarget) {
            configTarget.setConfiguration(context.getConfiguration());
        }
        if (bean instanceof SharedDataTarget dataTarget) {
            dataTarget.setSharedData(context.getSharedData());
        }
    }

    /**
     * 需要注入 PluginContext 的 Bean 接口。
     */
    public interface PluginContextAware {
        void setPluginContext(PluginContext context);
    }

    /**
     * 需要注入 PluginConfiguration 的 Bean 接口。
     */
    public interface PluginConfigurationTarget {
        void setConfiguration(PluginConfiguration configuration);
    }

    /**
     * 需要注入 SharedData 的 Bean 接口。
     */
    public interface SharedDataTarget {
        void setSharedData(SharedData sharedData);
    }
}
