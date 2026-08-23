package cn.jowen.framework.data.core.support;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 实体基类，提供主键字段。各业务实体继承此类以获得统一的主键管理。
 *
 * @param <ID> 主键类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public abstract class EntityBase<ID> {

    /**
     * 主键值，由各子类实现并提供对应字段。
     *
     * @return 主键，可为 {@code null}（未持久化时）
     */
    @Nullable
    public abstract ID getId();

    /**
     * 设置主键。
     *
     * @param id 主键，可为 {@code null}
     */
    public abstract void setId(ID id);
}
