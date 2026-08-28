package cn.jowen.framework.data.core.callback;

import org.jspecify.annotations.NullMarked;

/**
 * 实体生命周期回调，在实体持久化前后触发。
 *
 * @param <T> 实体类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface EntityCallback<T> {

    /**
     * 实体插入前回调。
     *
     * @param entity 待插入实体，不可为 {@code null}
     */
    default void beforeInsert(T entity) {
    }

    /**
     * 实体更新前回调。
     *
     * @param entity 待更新实体，不可为 {@code null}
     */
    default void beforeUpdate(T entity) {
    }

    /**
     * 实体插入后回调。
     *
     * @param entity 刚插入的实体，不可为 {@code null}
     */
    default void afterInsert(T entity) {
    }

    /**
     * 实体更新后回调。
     *
     * @param entity 刚更新的实体，不可为 {@code null}
     */
    default void afterUpdate(T entity) {
    }
}
