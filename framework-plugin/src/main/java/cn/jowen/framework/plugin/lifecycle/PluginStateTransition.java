package cn.jowen.framework.plugin.lifecycle;

import cn.jowen.framework.plugin.api.PluginState;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 插件状态转换规则。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginStateTransition {

    /**
     * 检查从 {@code from} 到 {@code to} 的状态转换是否合法。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return {@code true} 表示转换合法
     */
    public static boolean isValid(PluginState from, PluginState to) {
        if (to == null) return false;
        return from.canTransitTo(to);
    }

    /**
     * 执行状态转换，非法时抛出异常。
     *
     * @param current 当前状态原子引用
     * @param target  目标状态
     * @throws IllegalStateException 转换不合法时抛出
     */
    public static void transition(AtomicReference<PluginState> current, PluginState target) {
        PluginState from = current.get();
        if (!isValid(from, target)) {
            throw new IllegalStateException(
                    "非法状态转换: " + from + " -> " + target);
        }
        current.set(target);
    }
}
