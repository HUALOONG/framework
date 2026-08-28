package cn.jowen.framework.data.core.support;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 审计实体基类，继承 {@link VersionedEntity}，增加创建/更新时间和操作者字段。
 *
 * <p>典型字段：
 * <ul>
 *   <li>{@code createdBy} — 创建人标识</li>
 *   <li>{@code createTime} — 创建时间</li>
 *   <li>{@code updatedBy} — 最后更新人标识</li>
 *   <li>{@code updateTime} — 最后更新时间</li>
 * </ul>
 *
 * @param <ID> 主键类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class AuditableEntity<ID> extends VersionedEntity<ID> {

    @Nullable private String createdBy;
    private Instant createTime;
    @Nullable private String updatedBy;
    private Instant updateTime;

    /**
     * 获取创建人标识。
     *
     * @return 创建人，可为 {@code null}
     */
    @Nullable
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置创建人标识。
     *
     * @param createdBy 创建人
     */
    public void setCreatedBy(@Nullable String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public Instant getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间（通常在插入前由审计拦截器自动填充）。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取最后更新人标识。
     *
     * @return 更新人，可为 {@code null}
     */
    @Nullable
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 设置最后更新人标识。
     *
     * @param updatedBy 更新人
     */
    public void setUpdatedBy(@Nullable String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * 获取最后更新时间。
     *
     * @return 更新时间
     */
    public Instant getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置最后更新时间（通常在更新前由审计拦截器自动填充）。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }
}
