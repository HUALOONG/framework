package cn.jowen.framework.extras.web.properties;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.jowen.framework.extras.properties.ExtrasProperties;
import cn.jowen.framework.extras.properties.RateLimitAlgorithm;
import org.jspecify.annotations.NullMarked;

/**
 * Web 扩展配置属性模型。
 *
 * <p><b>本类是纯 POJO，不携带 {@code @ConfigurationProperties}。</b>
 * 真实的前缀绑定在
 * {@code framework-boot-autoconfigure} 的 {@code BootWebExtrasProperties}
 * （{@code prefix = "framework.extras.web"}）上，后者继承本类。
 * 这样拆分是为了让 Web 实现层保持零 Spring 依赖，与
 * {@code BootLoggerProperties} / {@code BootI18nProperties} 等采用同一模式。
 *
 * <p>本模块内仅借用其嵌套的 {@code Captcha.CaptchaType} 枚举作为验证码类型判别，
 * 与 Spring 属性绑定无关。</p>
 *
 * <p>该配置仅装配 Web 相关能力（签名、幂等、限流、数据权限等），
 * 与通用能力配置 {@link ExtrasProperties} 相互独立。</p>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ExtrasWebProperties {

    /** 总开关：Web 扩展整体是否启用（默认开启）。 */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 请求签名拦截与验签配置（默认开启）。 */
    private Sign sign = new Sign();

    /** 请求体加解密（AES-GCM）配置（默认开启）。 */
    private Crypto crypto = new Crypto();

    /** 限流配置（默认开启）。 */
    private RateLimit rateLimit = new RateLimit();

    /** 幂等配置（默认开启）。 */
    private Idempotent idempotent = new Idempotent();

    /** 分布式锁配置（默认开启，需 Redis）。 */
    private Lock lock = new Lock();

    /** 验证码配置（默认关闭，由 WebExtrasAutoConfiguration 按需装配）。 */
    private Captcha captcha = new Captcha();

    /** 数据权限配置（默认关闭）。 */
    private DataPermission dataPermission = new DataPermission();

    /** 操作日志配置（默认关闭）。 */
    private OperateLog operateLog = new OperateLog();

    /** 通知配置（默认关闭）。 */
    private Notification notification = new Notification();

    /** 字段脱敏配置（默认关闭）。 */
    private Desensitize desensitize = new Desensitize();

    /** Excel 导入导出配置（默认关闭）。 */
    private Excel excel = new Excel();

    /** IP 地理位置（ip2region）配置（默认关闭）。 */
    private Ip2Region ip2Region = new Ip2Region();

    public Sign getSign() {
        return sign;
    }

    public void setSign(Sign sign) {
        this.sign = sign;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Idempotent getIdempotent() {
        return idempotent;
    }

    public void setIdempotent(Idempotent idempotent) {
        this.idempotent = idempotent;
    }

    public Lock getLock() {
        return lock;
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

    public Captcha getCaptcha() {
        return captcha;
    }

    public void setCaptcha(Captcha captcha) {
        this.captcha = captcha;
    }

    public DataPermission getDataPermission() {
        return dataPermission;
    }

    public void setDataPermission(DataPermission dataPermission) {
        this.dataPermission = dataPermission;
    }

    public OperateLog getOperateLog() {
        return operateLog;
    }

    public void setOperateLog(OperateLog operateLog) {
        this.operateLog = operateLog;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Desensitize getDesensitize() {
        return desensitize;
    }

    public void setDesensitize(Desensitize desensitize) {
        this.desensitize = desensitize;
    }

    public Excel getExcel() {
        return excel;
    }

    public void setExcel(Excel excel) {
        this.excel = excel;
    }

    public Ip2Region getIp2Region() {
        return ip2Region;
    }

    public void setIp2Region(Ip2Region ip2Region) {
        this.ip2Region = ip2Region;
    }

    /** 请求签名配置。 */
    @NullMarked
    public static class Sign {
        private boolean enabled = true;
        private Map<String, String> appSecrets = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, String> getAppSecrets() {
            return appSecrets;
        }

        public void setAppSecrets(Map<String, String> appSecrets) {
            this.appSecrets = appSecrets;
        }
    }

    /** 请求体加解密配置。 */
    @NullMarked
    public static class Crypto {
        private boolean enabled = true;
        private String defaultKeyAlias = "default";
        private Map<String, String> keys = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultKeyAlias() {
            return defaultKeyAlias;
        }

        public void setDefaultKeyAlias(String defaultKeyAlias) {
            this.defaultKeyAlias = defaultKeyAlias;
        }

        public Map<String, String> getKeys() {
            return keys;
        }

        public void setKeys(Map<String, String> keys) {
            this.keys = keys;
        }
    }

    /** 限流配置。 */
    @NullMarked
    public static class RateLimit {
        private boolean enabled = true;

        /** Redis 异常时是否放行（默认 true）。false=拒绝并抛 {@link cn.jowen.framework.extras.common.exception.ExtrasException}。 */
        private boolean failOpen = true;

        /** Redis key 前缀（默认 "ratelimit:"），必须以分隔符结尾。 */
        private String keyPrefix = "ratelimit:";

        /** 默认限流算法（默认 TOKEN_BUCKET）。 */
        private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isFailOpen() {
            return failOpen;
        }

        public void setFailOpen(boolean failOpen) {
            this.failOpen = failOpen;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public RateLimitAlgorithm getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(RateLimitAlgorithm algorithm) {
            this.algorithm = algorithm;
        }
    }

    /** 幂等配置。 */
    @NullMarked
    public static class Idempotent {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** 分布式锁配置。 */
    @NullMarked
    public static class Lock {
        private boolean enabled = true;
        private long defaultLeaseMillis = 30000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getDefaultLeaseMillis() {
            return defaultLeaseMillis;
        }

        public void setDefaultLeaseMillis(long defaultLeaseMillis) {
            this.defaultLeaseMillis = defaultLeaseMillis;
        }
    }

    /** 验证码配置（默认关闭）。 */
    @NullMarked
    public static class Captcha {
        /** 验证码类型。 */
        public enum CaptchaType {
            /** 图形验证码 */
            GRAPHIC,
            /** 算术验证码 */
            ARITHMETIC,
            /** 滑块验证码 */
            SLIDER,
            /** 短信验证码 */
            SMS
        }

        private boolean enabled = false;
        private CaptchaType type = CaptchaType.ARITHMETIC;
        private long expireSeconds = 120;
        private int length = 4;
        private int width = 120;
        private int height = 40;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public CaptchaType getType() {
            return type;
        }

        public void setType(CaptchaType type) {
            this.type = type;
        }

        public long getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }

    /** 数据权限配置（默认关闭）。 */
    @NullMarked
    public static class DataPermission {
        private boolean enabled = false;
        private String deptColumn = "dept_id";
        private String userColumn = "create_by";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDeptColumn() {
            return deptColumn;
        }

        public void setDeptColumn(String deptColumn) {
            this.deptColumn = deptColumn;
        }

        public String getUserColumn() {
            return userColumn;
        }

        public void setUserColumn(String userColumn) {
            this.userColumn = userColumn;
        }
    }

    /** 操作日志配置（默认关闭）。 */
    @NullMarked
    public static class OperateLog {
        /** 是否启用 */
        private boolean enabled = false;

        /** 是否异步 */
        private boolean async = true;

        /** 处理器 */
        private String handler = "log";

        /**
         * 是否启用。
         * @return 是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用。
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 是否异步。
         * @return 是否异步
         */
        public boolean isAsync() {
            return async;
        }

        /**
         * 设置是否异步。
         * @param async 是否异步
         */
        public void setAsync(boolean async) {
            this.async = async;
        }

        /**
         * 获取处理器。
         * @return 处理器
         */
        public String getHandler() {
            return handler;
        }

        /**
         * 设置处理器。
         * @param handler 处理器
         */
        public void setHandler(String handler) {
            this.handler = handler;
        }
    }

    /**
     * 通知配置（默认关闭）。
     *
     * <p><b>已废弃（N-003）。</b>与
     * {@link cn.jowen.framework.extras.properties.NotificationProperties} 同属占位保留结构，框架通知能力未落地。
     * 生产代码实际通过 {@code MessageServiceSmsCaptchaSender} 桥接短信网关，引用协议级
     * {@code ErrorCodeEnum.NOTIFICATION_SEND_FAILED("E2004")}，不依赖本嵌套配置。请勿在新代码中新增依赖。</p>
     *
     * @deprecated 消息通知能力未落地，仅作占位保留；新代码请勿依赖本配置块。
     */
    @Deprecated
    @NullMarked
    public static class Notification {
        /** 是否启用 */
        private boolean enabled = false;

        /**
         * 是否启用。
         * @return 是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用。
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** 字段脱敏配置（默认关闭）。 */
    @NullMarked
    public static class Desensitize {
        /** 是否启用 */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** Excel 导入导出配置（默认关闭）。 */
    @NullMarked
    public static class Excel {
        /** 是否启用 */
        private boolean enabled = false;

        /** 单次导入/导出的最大行数，超出直接拒绝，防止大结果集打爆内存。 */
        private int maxRows = 100000;

        /**
         * 是否启用。
         * @return 是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用。
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 单次导入/导出的最大行数。
         * @return 行数上限
         */
        public int getMaxRows() {
            return maxRows;
        }

        /**
         * 设置单次导入/导出的最大行数。
         * @param maxRows 行数上限，必须为正数
         */
        public void setMaxRows(int maxRows) {
            if (maxRows <= 0) throw new IllegalArgumentException("maxRows must be positive");
            this.maxRows = maxRows;
        }
    }

    /** IP 地理位置（ip2region）配置（默认关闭）。 */
    @NullMarked
    public static class Ip2Region {
        /** 是否启用 */
        private boolean enabled = false;

        /**
         * 是否启用。
         * @return 是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用。
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
