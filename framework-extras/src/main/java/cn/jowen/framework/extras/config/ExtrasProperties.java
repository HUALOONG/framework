package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 扩展能力总配置属性（纯 POJO）。绑定前缀 {@code framework.extras.*}，由
 * boot-autoconfigure 层的 {@code BootExtrasProperties} 统一装配。
 *
 * <p>各子能力（lock / ratelimit / idempotent / captcha / storage / notification /
 * excel / ip2region / desensitize / operatelog / datapermission）的 {@code *Properties}
 * 作为嵌套类存放于此，对应 {@code framework.extras.lock.*}、{@code framework.extras.ratelimit.*} 等
 * 配置前缀。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class ExtrasProperties {

    /** 是否启用扩展能力装配，缺省开启。 */
    private boolean enabled = true;

    /** 分布式锁配置。 */
    private final LockProperties lock = new LockProperties();

    /** 接口限流配置。 */
    private final RateLimitProperties ratelimit = new RateLimitProperties();

    /** 幂等控制配置。 */
    private final IdempotentProperties idempotent = new IdempotentProperties();

    /** 验证码配置。 */
    private final CaptchaProperties captcha = new CaptchaProperties();

    /** 文件存储配置。 */
    private final StorageProperties storage = new StorageProperties();

    /** 消息通知配置。 */
    private final NotificationProperties notification = new NotificationProperties();

    /** Excel 处理配置。 */
    private final ExcelProperties excel = new ExcelProperties();

    /** IP 地域解析配置。 */
    private final Ip2RegionProperties ip2region = new Ip2RegionProperties();

    /** 数据脱敏配置。 */
    private final DesensitizeProperties desensitize = new DesensitizeProperties();

    /** 操作日志配置。 */
    private final OperateLogProperties operatelog = new OperateLogProperties();

    /** 数据权限配置。 */
    private final DataPermissionProperties datapermission = new DataPermissionProperties();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LockProperties getLock() { return lock; }
    public RateLimitProperties getRatelimit() { return ratelimit; }
    public IdempotentProperties getIdempotent() { return idempotent; }
    public CaptchaProperties getCaptcha() { return captcha; }
    public StorageProperties getStorage() { return storage; }
    public NotificationProperties getNotification() { return notification; }
    public ExcelProperties getExcel() { return excel; }
    public Ip2RegionProperties getIp2region() { return ip2region; }
    public DesensitizeProperties getDesensitize() { return desensitize; }
    public OperateLogProperties getOperatelog() { return operatelog; }
    public DataPermissionProperties getDatapermission() { return datapermission; }
}
