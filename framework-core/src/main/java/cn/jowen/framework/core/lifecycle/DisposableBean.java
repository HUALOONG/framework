package cn.jowen.framework.core.lifecycle;

import org.jspecify.annotations.NullMarked;

/**
 * 销毁回调接口，等价于 Spring 同名接口语义。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface DisposableBean {

    /**
     * 容器关闭时执行资源释放。
     *
     * @throws Exception 释放失败时抛出
     */
    void destroy() throws Exception;
}
