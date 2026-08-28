package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * Excel 处理配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ExcelProperties {

    /** enabled 字段。 */
    private boolean enabled = false;

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
}
