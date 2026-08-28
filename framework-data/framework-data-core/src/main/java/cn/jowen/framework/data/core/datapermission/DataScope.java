package cn.jowen.framework.data.core.datapermission;

import org.jspecify.annotations.NullMarked;

/**
 * 数据权限范围枚举。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum DataScope {

    /** 全部数据（无过滤） */
    ALL,

    /** 仅本人数据（创建人 = 当前用户） */
    SELF,

    /** 仅本部门数据 */
    DEPARTMENT,

    /** 本部门及下级部门数据 */
    DEPARTMENT_AND_CHILD,

    /** 自定义规则（由 {@link DataPermissionRule} 完全决定） */
    CUSTOM
}
