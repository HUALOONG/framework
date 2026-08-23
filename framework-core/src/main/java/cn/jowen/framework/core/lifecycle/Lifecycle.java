package cn.jowen.framework.core.lifecycle;

import org.jspecify.annotations.NullMarked;

/**
 * 组合生命周期接口，同时具备初始化与销毁能力。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public interface Lifecycle extends InitializingBean, DisposableBean {
}
