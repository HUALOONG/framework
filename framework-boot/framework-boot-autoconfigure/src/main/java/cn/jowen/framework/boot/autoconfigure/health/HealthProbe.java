package cn.jowen.framework.boot.autoconfigure.health;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.health.contributor.Health;
import org.springframework.context.ApplicationContext;

/**
 * 健康探测扩展点：向框架健康指示器补充可选模块的健康信息。
 *
 * <p><b>存在意义</b>：{@code framework-plugin} 等模块为可选依赖。若
 * {@link BootHealthIndicator} 直接在 {@code health()} 中引用 {@code PluginManager}，
 * 则执行 {@code context.getBeanProvider(PluginManager.class)} 时会触发该类的加载，
 * 在模块缺失时抛 {@code NoClassDefFoundError}——{@code getIfAvailable()} 的空值判断
 * 发生在类加载之后，无法起到保护作用。
 *
 * <p>将此类可选依赖的探测逻辑下沉到由 {@code @ConditionalOnClass} 保护的配置类中，
 * 可保证「未引入可选模块时健康检查依然可用」。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface HealthProbe {

    /**
     * 执行探测并写入健康明细。
     *
     * @param builder 健康信息构建器，不可为 {@code null}；探测到致命问题时调用 {@code builder.down()}
     * @param context Spring 上下文，不可为 {@code null}
     */
    void probe(Health.Builder builder, ApplicationContext context);
}
