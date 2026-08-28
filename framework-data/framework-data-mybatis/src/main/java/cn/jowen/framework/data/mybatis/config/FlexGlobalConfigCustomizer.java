package cn.jowen.framework.data.mybatis.config;

import org.jspecify.annotations.NullMarked;

/**
 * MyBatis Flex 全局配置定制回调。业务方可实现此接口自定义 MyBatis Flex 全局参数。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface FlexGlobalConfigCustomizer {

    /**
     * 定制 MyBatis Flex 全局配置对象。
     *
     * @param config 全局配置，不可为 {@code null}
     */
    void customize(Object config);
}
