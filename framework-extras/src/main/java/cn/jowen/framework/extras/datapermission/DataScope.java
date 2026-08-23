package cn.jowen.framework.extras.datapermission;

import org.jspecify.annotations.NullMarked;

/**
 * 数据权限范围枚举。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public enum DataScope {
    /**
     * 全部数据。
     */
    ALL,
    /**
     * 本部门及子部门。
     */
    DEPT_AND_CHILD,
    /**
     * 仅本部门。
     */
    DEPT,
    /**
     * 仅本人。
     */
    SELF,
    /**
     * 自定义。
     */
    CUSTOM
}
