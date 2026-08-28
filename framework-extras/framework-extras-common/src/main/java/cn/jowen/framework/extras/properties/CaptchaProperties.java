package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 验证码配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class CaptchaProperties {

    /** enabled 字段。 */
    private boolean enabled = false;
    /** type 字段。 */
    private CaptchaType type = CaptchaType.ARITHMETIC;
    /** width 字段。 */
    private int width = 120;
    /** height 字段。 */
    private int height = 40;
    /** length 字段。 */
    private int length = 4;
    /** expireSeconds 字段。 */
    private long expireSeconds = 120L;

    /**
     * 「CaptchaType」枚举定义。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public enum CaptchaType {
        /** SMS 字段。 */
        GRAPHIC, ARITHMETIC, SLIDER, SMS
    }

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
     * 获取type。
     * @return 结果
     */
    public CaptchaType getType() {
        return type;
    }

    /**
     * 设置type。
     * @param type 参数 type
     */
    public void setType(CaptchaType type) {
        this.type = type;
    }

    /**
     * 获取width。
     * @return 结果
     */
    public int getWidth() {
        return width;
    }

    /**
     * 设置width。
     * @param width 参数 width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * 获取height。
     * @return 结果
     */
    public int getHeight() {
        return height;
    }

    /**
     * 设置height。
     * @param height 参数 height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * 获取length。
     * @return 结果
     */
    public int getLength() {
        return length;
    }

    /**
     * 设置length。
     * @param length 参数 length
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * 获取expire seconds。
     * @return 结果
     */
    public long getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 设置expire seconds。
     * @param expireSeconds 参数 expireSeconds
     */
    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
