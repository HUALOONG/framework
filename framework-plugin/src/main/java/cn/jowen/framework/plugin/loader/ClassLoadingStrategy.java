package cn.jowen.framework.plugin.loader;

import org.jspecify.annotations.NullMarked;

/**
 * 类加载策略枚举。
 *
 * @author 王飞
 */
@NullMarked
public enum ClassLoadingStrategy {

    /**
     * 父加载器优先（传统双亲委派）。
     */
    PARENT_FIRST,

    /**
     * 子加载器优先（插件类优先于宿主）。
     */
    CHILD_FIRST,

    /**
     * 框架 API 委派模式：cn.jowen.framework.** 和 cn.jowen.framework.plugin.api.** 委派给父加载器，其余插件私有类隔离加载。推荐默认策略。
     */
    FRAMEWORK_API_DELEGATE
}
