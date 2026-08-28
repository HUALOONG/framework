package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 数据权限配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class DataPermissionProperties {

    /** enabled 字段。 */
    private boolean enabled = false;
    /** defaultScope 字段。 */
    private DataScope defaultScope = DataScope.ALL;

    /**
     * 获取enabled。
     * @return 结果
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置enabled。
     * @param enabled 参数 enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取default scope。
     * @return 结果
     */
    public DataScope getDefaultScope() {
        return defaultScope;
    }

    /**
     * 设置default scope。
     * @param defaultScope 参数 defaultScope
     */
    public void setDefaultScope(DataScope defaultScope) {
        this.defaultScope = defaultScope;
    }
}
