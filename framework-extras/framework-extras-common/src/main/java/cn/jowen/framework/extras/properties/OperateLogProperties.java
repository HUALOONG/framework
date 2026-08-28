package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 操作日志配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class OperateLogProperties {

    /** enabled 字段。 */
    private boolean enabled = false;
    /** async 字段。 */
    private boolean async = true;
    /** handler 字段。 */
    private String handler = "log";

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
     * 获取async。
     * @return 结果
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * 设置async。
     * @param async 参数 async
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * 获取handler。
     * @return 结果
     */
    public String getHandler() {
        return handler;
    }

    /**
     * 设置handler。
     * @param handler 参数 handler
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }
}
