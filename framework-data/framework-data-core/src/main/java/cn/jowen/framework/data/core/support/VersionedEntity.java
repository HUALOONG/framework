package cn.jowen.framework.data.core.support;

import org.jspecify.annotations.NullMarked;

/**
 * 乐观锁版本实体基类，继承 {@link EntityBase}，增加 {@code version} 字段。
 *
 * <p>更新时需满足 {@code WHERE id = ? AND version = ?}，实现行级乐观锁。
 *
 * @param <ID> 主键类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class VersionedEntity<ID> extends EntityBase<ID> {

    /**
     * 乐观锁版本号，由实现层自动管理。
     *
     * @return 版本号，默认为 0
     */
    public int getVersion() {
        return 0;
    }

    /**
     * 设置版本号（内部使用）。
     *
     * @param version 版本号
     */
    public void setVersion(int version) {
    }

    /**
     * 版本加一（提交后调用）。
     */
    public void incrementVersion() {
        setVersion(getVersion() + 1);
    }
}
