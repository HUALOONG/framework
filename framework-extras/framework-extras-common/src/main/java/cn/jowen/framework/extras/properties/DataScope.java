package cn.jowen.framework.extras.properties;

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

    /** 全量数据（无限制） */
    ALL,

    /** 仅本人数据 */
    SELF,

    /** 仅本部门数据 */
    DEPT,

    /** 本部门及子部门 */
    DEPT_AND_CHILD,

    /** 自定义规则 */
    CUSTOM
}
