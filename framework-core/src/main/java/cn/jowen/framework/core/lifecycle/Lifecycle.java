package cn.jowen.framework.core.lifecycle;

import org.jspecify.annotations.NullMarked;

/**
 * 组合生命周期接口，同时具备初始化与销毁能力。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface Lifecycle extends InitializingBean, DisposableBean {
}
