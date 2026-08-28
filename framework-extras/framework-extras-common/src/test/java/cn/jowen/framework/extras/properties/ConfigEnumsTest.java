package cn.jowen.framework.extras.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * config 包内各枚举的完整性契约测试。
 *
 * <p>枚举常量集合是配置文件（yaml）的可选值来源，改名/删项属于破坏性变更，故在此加锁。
 */
class ConfigEnumsTest {

    @Test
    void dataScope_containsExpectedConstants() {
        assertThat(DataScope.values()).containsExactly(
                DataScope.ALL,
                DataScope.SELF,
                DataScope.DEPT,
                DataScope.DEPT_AND_CHILD,
                DataScope.CUSTOM);
    }

    @Test
    void lockType_containsExpectedConstants() {
        assertThat(LockType.values()).containsExactly(
                LockType.REENTRANT,
                LockType.FAIR,
                LockType.READ,
                LockType.WRITE,
                LockType.MULTI,
                LockType.RED);
    }

    @Test
    void namingStrategy_containsExpectedConstants() {
        assertThat(NamingStrategy.values()).containsExactly(
                NamingStrategy.ORIGINAL,
                NamingStrategy.UUID,
                NamingStrategy.DATE,
                NamingStrategy.HASH,
                NamingStrategy.CUSTOM);
    }

    @Test
    void rateLimitAlgorithm_containsExpectedConstants() {
        assertThat(RateLimitAlgorithm.values()).containsExactly(
                RateLimitAlgorithm.FIXED_WINDOW,
                RateLimitAlgorithm.SLIDING_WINDOW,
                RateLimitAlgorithm.LEAKY_BUCKET,
                RateLimitAlgorithm.TOKEN_BUCKET);
    }

    @Test
    void storageType_containsExpectedConstants() {
        assertThat(StorageType.values()).containsExactly(
                StorageType.LOCAL,
                StorageType.OSS,
                StorageType.S3,
                StorageType.MINIO);
    }

    @Test
    void captchaType_containsExpectedConstants() {
        assertThat(CaptchaProperties.CaptchaType.values()).containsExactly(
                CaptchaProperties.CaptchaType.GRAPHIC,
                CaptchaProperties.CaptchaType.ARITHMETIC,
                CaptchaProperties.CaptchaType.SLIDER,
                CaptchaProperties.CaptchaType.SMS);
    }

    @Test
    void ip2RegionLoadType_containsExpectedConstants() {
        assertThat(Ip2RegionProperties.LoadType.values()).containsExactly(
                Ip2RegionProperties.LoadType.MEMORY,
                Ip2RegionProperties.LoadType.INDEX,
                Ip2RegionProperties.LoadType.FILE);
    }

    @Test
    void enums_resolveByNameForConfigurationBinding() {
        assertThat(StorageType.valueOf("MINIO")).isSameAs(StorageType.MINIO);
        assertThat(LockType.valueOf("RED")).isSameAs(LockType.RED);
        assertThat(RateLimitAlgorithm.valueOf("SLIDING_WINDOW")).isSameAs(RateLimitAlgorithm.SLIDING_WINDOW);
        assertThat(DataScope.valueOf("DEPT_AND_CHILD")).isSameAs(DataScope.DEPT_AND_CHILD);
        assertThat(Ip2RegionProperties.LoadType.valueOf("INDEX"))
                .isSameAs(Ip2RegionProperties.LoadType.INDEX);
    }

    @Test
    void enums_rejectUnknownName() {
        assertThatThrownBy(() -> StorageType.valueOf("FTP"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
