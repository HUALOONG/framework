package cn.jowen.framework.core.lifecycle;

import cn.jowen.framework.core.exception.SystemException;
import org.jspecify.annotations.NullMarked;

/**
 * 初始化回调接口，等价于 Spring 同名接口语义，使框架组件在脱离 Spring 时也能完成初始化。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@FunctionalInterface
public interface InitializingBean {

    /**
     * Bean 属性设置完成后执行。失败抛 {@link SystemException}。
     *
     * @throws SystemException 初始化失败时抛出
     */
    void afterPropertiesSet() throws SystemException;
}
