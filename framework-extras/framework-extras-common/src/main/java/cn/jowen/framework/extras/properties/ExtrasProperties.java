package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 扩展能力总配置属性（纯 POJO，不依赖 Spring）。
 *
 * <p>聚合 12 项能力的开关与参数，由 Boot 装配层的 {@code BootExtrasProperties}
 * 继承并标注 {@code @ConfigurationProperties("framework.extras")}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ExtrasProperties {

    /** 总开关 */
    private boolean enabled = true;

    /** lock 不可变字段。 */
    private final LockProperties lock = new LockProperties();
    /** ratelimit 不可变字段。 */
    private final RateLimitProperties ratelimit = new RateLimitProperties();
    /** idempotent 不可变字段。 */
    private final IdempotentProperties idempotent = new IdempotentProperties();
    /** captcha 不可变字段。 */
    private final CaptchaProperties captcha = new CaptchaProperties();
    /** storage 不可变字段。 */
    private final StorageProperties storage = new StorageProperties();
    /** notification 不可变字段。 */
    private final NotificationProperties notification = new NotificationProperties();
    /** excel 不可变字段。 */
    private final ExcelProperties excel = new ExcelProperties();
    /** ip2region 不可变字段。 */
    private final Ip2RegionProperties ip2region = new Ip2RegionProperties();
    /** desensitize 不可变字段。 */
    private final DesensitizeProperties desensitize = new DesensitizeProperties();
    /** operatelog 不可变字段。 */
    private final OperateLogProperties operatelog = new OperateLogProperties();
    /** datapermission 不可变字段。 */
    private final DataPermissionProperties datapermission = new DataPermissionProperties();

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
     * 获取lock。
     * @return 结果
     */
    public LockProperties getLock() {
        return lock;
    }

    /**
     * 获取ratelimit。
     * @return 结果
     */
    public RateLimitProperties getRatelimit() {
        return ratelimit;
    }

    /**
     * 获取idempotent。
     * @return 结果
     */
    public IdempotentProperties getIdempotent() {
        return idempotent;
    }

    /**
     * 获取captcha。
     * @return 结果
     */
    public CaptchaProperties getCaptcha() {
        return captcha;
    }

    /**
     * 获取storage。
     * @return 结果
     */
    public StorageProperties getStorage() {
        return storage;
    }

    /**
     * 获取notification。
     * @return 结果
     */
    public NotificationProperties getNotification() {
        return notification;
    }

    /**
     * 获取excel。
     * @return 结果
     */
    public ExcelProperties getExcel() {
        return excel;
    }

    /**
     * 获取ip2region。
     * @return 结果
     */
    public Ip2RegionProperties getIp2region() {
        return ip2region;
    }

    /**
     * 获取desensitize。
     * @return 结果
     */
    public DesensitizeProperties getDesensitize() {
        return desensitize;
    }

    /**
     * 获取operatelog。
     * @return 结果
     */
    public OperateLogProperties getOperatelog() {
        return operatelog;
    }

    /**
     * 获取datapermission。
     * @return 结果
     */
    public DataPermissionProperties getDatapermission() {
        return datapermission;
    }
}
