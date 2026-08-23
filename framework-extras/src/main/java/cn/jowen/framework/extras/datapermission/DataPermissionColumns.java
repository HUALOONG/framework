package cn.jowen.framework.extras.datapermission;

import org.jspecify.annotations.NullMarked;

/**
 * 数据权限列名常量（白名单来源）。
 *
 * <p>规则引擎只使用本类声明的列名拼接 SQL，禁止外部注入任意列名/表名，以防范 SQL 注入。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class DataPermissionColumns {

    /**
     * 部门列名。
     */
    public static final String DEPT_COLUMN = "dept_id";

    /**
     * 用户列名。
     */
    public static final String USER_COLUMN = "create_by";

    private DataPermissionColumns() {
    }
}
