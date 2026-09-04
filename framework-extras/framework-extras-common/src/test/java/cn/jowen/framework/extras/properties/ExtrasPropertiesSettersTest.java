package cn.jowen.framework.extras.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 各 Properties 配置项的 setter 写入契约测试。
 *
 * <p>默认值契约由 {@link ExtrasPropertiesTest} 守护，本类只验证"写入后可读回"这一绑定语义：
 * Spring {@code @ConfigurationProperties} 绑定依赖 setter 生效，setter 失效等于配置静默丢失，
 * 且不会有任何编译期或启动期报错。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ExtrasPropertiesSettersTest {

    @Test
    void extrasRoot_setEnabled_roundTrips() {
        ExtrasProperties props = new ExtrasProperties();

        props.setEnabled(false);

        assertThat(props.isEnabled()).isFalse();
    }

    @Test
    void desensitize_setEnabled_roundTrips() {
        DesensitizeProperties props = new DesensitizeProperties();

        props.setEnabled(true);

        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void excel_setEnabled_roundTrips() {
        ExcelProperties props = new ExcelProperties();

        props.setEnabled(true);

        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void storage_setEnabled_roundTrips() {
        StorageProperties props = new StorageProperties();

        props.setEnabled(false);

        assertThat(props.isEnabled()).isFalse();
    }

    @Test
    void idempotent_setters_roundTrip() {
        IdempotentProperties props = new IdempotentProperties();

        props.setEnabled(false);
        props.setDefaultExpireSeconds(60L);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getDefaultExpireSeconds()).isEqualTo(60L);
    }

    @Test
    void dataPermission_setters_roundTrip() {
        DataPermissionProperties props = new DataPermissionProperties();

        props.setEnabled(true);
        props.setDefaultScope(DataScope.SELF);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getDefaultScope()).isEqualTo(DataScope.SELF);
    }

    @Test
    void rateLimit_setters_roundTrip() {
        RateLimitProperties props = new RateLimitProperties();

        props.setEnabled(false);
        props.setDefaultAlgorithm(RateLimitAlgorithm.LEAKY_BUCKET);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getDefaultAlgorithm()).isEqualTo(RateLimitAlgorithm.LEAKY_BUCKET);
    }

    @Test
    void operateLog_setters_roundTrip() {
        OperateLogProperties props = new OperateLogProperties();

        props.setEnabled(true);
        props.setAsync(false);
        props.setHandler("audit");

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.isAsync()).isFalse();
        assertThat(props.getHandler()).isEqualTo("audit");
    }

    @Test
    void ip2Region_setters_roundTrip() {
        Ip2RegionProperties props = new Ip2RegionProperties();

        props.setEnabled(true);
        props.setLoadType(Ip2RegionProperties.LoadType.FILE);
        props.setDbPath("/data/ip2region.xdb");

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getLoadType()).isEqualTo(Ip2RegionProperties.LoadType.FILE);
        assertThat(props.getDbPath()).isEqualTo("/data/ip2region.xdb");
    }
}
