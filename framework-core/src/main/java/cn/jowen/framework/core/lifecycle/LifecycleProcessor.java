package cn.jowen.framework.core.lifecycle;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 生命周期处理器：统一管理一组 {@link Lifecycle}，支持按阶段排序的批量启动/停止。
 *
 * <p>语义约定：
 * <ul>
 *   <li>{@link #startAll()}：{@link SmartLifecycle} 按 {@link SmartLifecycle#getPhase()} 升序启动
 *       （仅 {@link SmartLifecycle#isAutoStartup()} 为 true 者自动启动）；普通 {@link Lifecycle}
 *       执行初始化 {@link Lifecycle#afterPropertiesSet()}；</li>
 *   <li>{@link #stopAll()}：{@link SmartLifecycle} 按阶段降序停止；普通 {@link Lifecycle}
 *       执行销毁 {@link Lifecycle#destroy()}；</li>
 *   <li>线程安全：内部使用 {@link CopyOnWriteArrayList}，可随时注册/移除，不影响正在进行的启停遍历。</li>
 * </ul>
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class LifecycleProcessor {

    private static final Comparator<Lifecycle> PHASE_ASC =
            Comparator.comparingInt(LifecycleProcessor::phaseOf).reversed()
                    .reversed() // 保持可读：按 phase 升序
                    .thenComparing(System::identityHashCode);
    private final List<Lifecycle> lifecycles = new CopyOnWriteArrayList<>();

    private static int phaseOf(Lifecycle lifecycle) {
        return lifecycle instanceof SmartLifecycle smart ? smart.getPhase() : SmartLifecycle.DEFAULT_PHASE;
    }

    /**
     * 注册一个生命周期组件。
     */
    public void addLifecycle(Lifecycle lifecycle) {
        if (lifecycle != null) {
            lifecycles.add(lifecycle);
        }
    }

    /**
     * 移除一个生命周期组件。
     */
    public void removeLifecycle(Lifecycle lifecycle) {
        lifecycles.remove(lifecycle);
    }

    /**
     * 启动/初始化全部组件：{@link SmartLifecycle} 按阶段升序启动（仅自动启动者），
     * 普通 {@link Lifecycle} 执行初始化。
     */
    public void startAll() {
        sorted().forEach(lifecycle -> {
            if (lifecycle instanceof SmartLifecycle smart) {
                if (smart.isAutoStartup()) {
                    smart.start();
                }
            } else {
                lifecycle.afterPropertiesSet();
            }
        });
    }

    /**
     * 停止/销毁全部组件：{@link SmartLifecycle} 按阶段降序停止（自动启动者），
     * 普通 {@link Lifecycle} 执行销毁。
     */
    public void stopAll() {
        List<Lifecycle> ordered = sorted();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Lifecycle lifecycle = ordered.get(i);
            if (lifecycle instanceof SmartLifecycle smart) {
                if (smart.isAutoStartup()) {
                    smart.stop();
                }
            } else {
                try {
                    lifecycle.destroy();
                } catch (Exception e) {
                    throw new cn.jowen.framework.core.exception.SystemException(
                            "组件销毁失败: " + lifecycle.getClass().getName(), e);
                }
            }
        }
    }

    /**
     * 是否全部已注册的 {@link SmartLifecycle} 均在运行中（无 SmartLifecycle 时返回 true）。
     */
    public boolean isRunning() {
        return lifecycles.stream()
                .filter(SmartLifecycle.class::isInstance)
                .map(SmartLifecycle.class::cast)
                .allMatch(SmartLifecycle::isRunning);
    }

    /**
     * 当前管理的全部组件（拷贝）。
     */
    public List<Lifecycle> getLifecycles() {
        return new ArrayList<>(lifecycles);
    }

    /**
     * 已注册组件数量。
     */
    public int size() {
        return lifecycles.size();
    }

    /**
     * 返回已注册组件的拷贝，并按阶段升序排序。
     */
    private List<Lifecycle> sorted() {
        List<Lifecycle> copy = new ArrayList<>(lifecycles);
        copy.sort(PHASE_ASC);
        return copy;
    }
}
