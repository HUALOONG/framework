package cn.jowen.framework.extras.config;

import cn.jowen.framework.extras.datapermission.DataScope;
import org.jspecify.annotations.NullMarked;

/**
 * 数据权限配置属性。
 *
 * <p>通过 {@code framework.extras.datapermission.*} 前缀绑定，由 boot-autoconfigure 统一装配。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class DataPermissionProperties {

    /** 是否启用数据权限，默认 true。 */
    private boolean enabled = true;

    /** 默认数据范围（未配置用户时回退），默认 ALL。 */
    private DataScope defaultScope = DataScope.ALL;

    /** 默认部门列名，默认 "dept_id"。 */
    private String defaultDeptColumn = "dept_id";

    /** 默认用户列名，默认 "create_by"。 */
    private String defaultUserColumn = "create_by";

    /** 忽略数据权限的表名列表。 */
    private java.util.List<String> ignoreTables = java.util.List.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public DataScope getDefaultScope() { return defaultScope; }
    public void setDefaultScope(DataScope defaultScope) { this.defaultScope = defaultScope; }

    public String getDefaultDeptColumn() { return defaultDeptColumn; }
    public void setDefaultDeptColumn(String defaultDeptColumn) { this.defaultDeptColumn = defaultDeptColumn; }

    public String getDefaultUserColumn() { return defaultUserColumn; }
    public void setDefaultUserColumn(String defaultUserColumn) { this.defaultUserColumn = defaultUserColumn; }

    public java.util.List<String> getIgnoreTables() { return ignoreTables; }
    public void setIgnoreTables(java.util.List<String> ignoreTables) { this.ignoreTables = ignoreTables == null ? java.util.List.of() : ignoreTables; }
}
