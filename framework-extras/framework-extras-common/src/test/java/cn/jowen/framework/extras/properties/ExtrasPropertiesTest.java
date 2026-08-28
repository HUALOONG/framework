package cn.jowen.framework.extras.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ExtrasProperties} 及各子配置的默认值契约测试。
 *
 * <p>默认值是对外行为约定（未配置时的框架表现），一旦变更会影响使用方，故加锁保护。
 * 纯 getter/setter 的透传不单独测试。
 */
class ExtrasPropertiesTest {

    // ---------- 聚合根 ----------

    @Test
    void extras_isEnabledByDefault() {
        assertThat(new ExtrasProperties().isEnabled()).isTrue();
    }

    @Test
    void extras_exposesAllNestedSectionsNonNull() {
        ExtrasProperties props = new ExtrasProperties();

        assertThat(props.getLock()).isNotNull();
        assertThat(props.getRatelimit()).isNotNull();
        assertThat(props.getIdempotent()).isNotNull();
        assertThat(props.getCaptcha()).isNotNull();
        assertThat(props.getStorage()).isNotNull();
        assertThat(props.getNotification()).isNotNull();
        assertThat(props.getExcel()).isNotNull();
        assertThat(props.getIp2region()).isNotNull();
        assertThat(props.getDesensitize()).isNotNull();
        assertThat(props.getOperatelog()).isNotNull();
        assertThat(props.getDatapermission()).isNotNull();
    }

    @Test
    void extras_nestedSectionsAreStableInstances() {
        ExtrasProperties props = new ExtrasProperties();
        // final 字段，多次获取必须是同一实例，否则 Spring 绑定的属性会丢失
        assertThat(props.getLock()).isSameAs(props.getLock());
        assertThat(props.getStorage()).isSameAs(props.getStorage());
        assertThat(props.getCaptcha()).isSameAs(props.getCaptcha());
    }

    @Test
    void extras_nestedSectionMutationIsVisibleThroughRoot() {
        ExtrasProperties props = new ExtrasProperties();
        props.getLock().setKeyPrefix("custom:");
        props.getStorage().setBucket("my-bucket");

        assertThat(props.getLock().getKeyPrefix()).isEqualTo("custom:");
        assertThat(props.getStorage().getBucket()).isEqualTo("my-bucket");
    }

    @Test
    void extras_instancesAreIndependent() {
        ExtrasProperties a = new ExtrasProperties();
        ExtrasProperties b = new ExtrasProperties();
        a.getLock().setKeyPrefix("a:");

        assertThat(b.getLock().getKeyPrefix()).isEqualTo("lock:");
    }

    // ---------- 各能力默认开关 ----------

    @Test
    void defaultEnabledFlags_matchContract() {
        ExtrasProperties props = new ExtrasProperties();

        // 默认开启的能力
        assertThat(props.getLock().isEnabled()).as("lock").isTrue();
        assertThat(props.getRatelimit().isEnabled()).as("ratelimit").isTrue();
        assertThat(props.getIdempotent().isEnabled()).as("idempotent").isTrue();
        assertThat(props.getStorage().isEnabled()).as("storage").isTrue();
        assertThat(props.getNotification().isEnabled()).as("notification").isTrue();

        // 默认关闭的能力（需显式开启）
        assertThat(props.getCaptcha().isEnabled()).as("captcha").isFalse();
        assertThat(props.getExcel().isEnabled()).as("excel").isFalse();
        assertThat(props.getIp2region().isEnabled()).as("ip2region").isFalse();
        assertThat(props.getDesensitize().isEnabled()).as("desensitize").isFalse();
        assertThat(props.getOperatelog().isEnabled()).as("operatelog").isFalse();
        assertThat(props.getDatapermission().isEnabled()).as("datapermission").isFalse();
    }

    // ---------- 明细默认值 ----------

    @Test
    void lock_defaults() {
        LockProperties lock = new LockProperties();

        assertThat(lock.isEnabled()).isTrue();
        assertThat(lock.getType()).isEqualTo(LockType.REENTRANT);
        assertThat(lock.getKeyPrefix()).isEqualTo("lock:");
        assertThat(lock.getDefaultLeaseTime()).isEqualTo(30_000L);
        assertThat(lock.isWatchdogEnabled()).isTrue();
    }

    @Test
    void rateLimit_defaults() {
        RateLimitProperties rateLimit = new RateLimitProperties();

        assertThat(rateLimit.isEnabled()).isTrue();
        assertThat(rateLimit.getDefaultAlgorithm()).isEqualTo(RateLimitAlgorithm.TOKEN_BUCKET);
    }

    @Test
    void idempotent_defaults() {
        IdempotentProperties idempotent = new IdempotentProperties();

        assertThat(idempotent.isEnabled()).isTrue();
        assertThat(idempotent.getDefaultExpireSeconds()).isEqualTo(300L);
    }

    @Test
    void captcha_defaults() {
        CaptchaProperties captcha = new CaptchaProperties();

        assertThat(captcha.isEnabled()).isFalse();
        assertThat(captcha.getType()).isEqualTo(CaptchaProperties.CaptchaType.ARITHMETIC);
        assertThat(captcha.getWidth()).isEqualTo(120);
        assertThat(captcha.getHeight()).isEqualTo(40);
        assertThat(captcha.getLength()).isEqualTo(4);
        assertThat(captcha.getExpireSeconds()).isEqualTo(120L);
    }

    @Test
    void storage_defaults() {
        StorageProperties storage = new StorageProperties();

        assertThat(storage.isEnabled()).isTrue();
        assertThat(storage.getType()).isEqualTo(StorageType.LOCAL);
        assertThat(storage.getNamingStrategy()).isEqualTo(NamingStrategy.UUID);
        assertThat(storage.getBasePath()).isEqualTo("/tmp/framework-extras-storage");
        // 凭据类字段默认空串而非 null，避免使用方 NPE
        assertThat(storage.getEndpoint()).isEmpty();
        assertThat(storage.getAccessKey()).isEmpty();
        assertThat(storage.getSecretKey()).isEmpty();
        assertThat(storage.getBucket()).isEmpty();
        assertThat(storage.getRegion()).isEmpty();
    }

    @Test
    void notification_defaults() {
        NotificationProperties notification = new NotificationProperties();

        assertThat(notification.isEnabled()).isTrue();
        assertThat(notification.getDefaultChannel()).isEqualTo("email");
    }

    @Test
    void ip2Region_defaults() {
        Ip2RegionProperties ip2region = new Ip2RegionProperties();

        assertThat(ip2region.isEnabled()).isFalse();
        assertThat(ip2region.getLoadType()).isEqualTo(Ip2RegionProperties.LoadType.MEMORY);
        assertThat(ip2region.getDbPath()).isEqualTo("ip2region.xdb");
    }

    @Test
    void operateLog_defaults() {
        OperateLogProperties operateLog = new OperateLogProperties();

        assertThat(operateLog.isEnabled()).isFalse();
        assertThat(operateLog.isAsync()).isTrue();
        assertThat(operateLog.getHandler()).isEqualTo("log");
    }

    @Test
    void dataPermission_defaults() {
        DataPermissionProperties dataPermission = new DataPermissionProperties();

        assertThat(dataPermission.isEnabled()).isFalse();
        assertThat(dataPermission.getDefaultScope()).isEqualTo(DataScope.ALL);
    }

    @Test
    void desensitizeAndExcel_defaultToDisabled() {
        assertThat(new DesensitizeProperties().isEnabled()).isFalse();
        assertThat(new ExcelProperties().isEnabled()).isFalse();
    }

    // ---------- setter 覆写默认值 ----------

    @Test
    void setters_overrideDefaults() {
        LockProperties lock = new LockProperties();
        lock.setEnabled(false);
        lock.setType(LockType.RED);
        lock.setKeyPrefix("app:lock:");
        lock.setDefaultLeaseTime(5_000L);
        lock.setWatchdogEnabled(false);

        assertThat(lock.isEnabled()).isFalse();
        assertThat(lock.getType()).isEqualTo(LockType.RED);
        assertThat(lock.getKeyPrefix()).isEqualTo("app:lock:");
        assertThat(lock.getDefaultLeaseTime()).isEqualTo(5_000L);
        assertThat(lock.isWatchdogEnabled()).isFalse();
    }

    @Test
    void storageSetters_overrideDefaults() {
        StorageProperties storage = new StorageProperties();
        storage.setType(StorageType.MINIO);
        storage.setNamingStrategy(NamingStrategy.DATE);
        storage.setBasePath("/data");
        storage.setEndpoint("http://localhost:9000");
        storage.setBucket("bucket");
        storage.setRegion("cn-north-1");
        storage.setAccessKey("ak");
        storage.setSecretKey("sk");

        assertThat(storage.getType()).isEqualTo(StorageType.MINIO);
        assertThat(storage.getNamingStrategy()).isEqualTo(NamingStrategy.DATE);
        assertThat(storage.getBasePath()).isEqualTo("/data");
        assertThat(storage.getEndpoint()).isEqualTo("http://localhost:9000");
        assertThat(storage.getBucket()).isEqualTo("bucket");
        assertThat(storage.getRegion()).isEqualTo("cn-north-1");
        assertThat(storage.getAccessKey()).isEqualTo("ak");
        assertThat(storage.getSecretKey()).isEqualTo("sk");
    }
}
