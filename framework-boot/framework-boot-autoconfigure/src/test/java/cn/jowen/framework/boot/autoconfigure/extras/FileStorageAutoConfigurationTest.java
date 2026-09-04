package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.properties.StorageProperties;
import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.FileStorageManager;
import cn.jowen.framework.extras.storage.local.LocalFileStorage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link FileStorageAutoConfiguration} 装配验证：确认 {@code framework.extras.storage.*}
 * 生效，且各后端仅在对应 SDK 存在且类型匹配时才装配。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class FileStorageAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration.class));

    @Test
    void storagePropertiesAreBound() {
        runner.withPropertyValues(
                        "framework.extras.storage.base-path=/data/uploads",
                        "framework.extras.storage.naming-strategy=DATE",
                        "framework.extras.storage.bucket=my-bucket")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    StorageProperties props = context.getBean(BootStorageProperties.class);
                    assertThat(props.getBasePath()).isEqualTo("/data/uploads");
                    assertThat(props.getNamingStrategy())
                            .isEqualTo(cn.jowen.framework.extras.properties.NamingStrategy.DATE);
                    assertThat(props.getBucket()).isEqualTo("my-bucket");
                });
    }

    @Test
    void localStorageIsDefault() {
        runner.run(context -> {
            StorageProperties props = context.getBean(BootStorageProperties.class);
            assertThat(props.getType()).isEqualTo(StorageType.LOCAL);
            assertThat(context).hasSingleBean(LocalFileStorage.class);
        });
    }

    @Test
    void managerBeanRegistered() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(FileStorageManager.class);
        });
    }

    @Test
    void cloudBackendsNotRegisteredWithoutSdk() {
        // 三个云后端 SDK 均为 optional，测试类路径下不存在，故不应注册任何云存储 Bean
        runner.withPropertyValues("framework.extras.storage.type=MINIO")
                .run(context -> assertThat(context).doesNotHaveBean("minioFileStorage"));

        runner.withPropertyValues("framework.extras.storage.type=OSS")
                .run(context -> assertThat(context).doesNotHaveBean("ossFileStorage"));

        runner.withPropertyValues("framework.extras.storage.type=S3")
                .run(context -> assertThat(context).doesNotHaveBean("s3FileStorage"));
    }

    @Test
    void managerBucketsAllStorageTypes_viaBucketNameOfSwitch() {
        // 注册 OSS/S3/MINIO 类型的自定义 FileStorage Bean（mock，无需云 SDK），
        // 触发 FileStorageAutoConfiguration.bucketNameOf 的 OSS/S3/MINIO 分支，使 switch 全覆盖
        runner.withUserConfiguration(MultiStorageConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FileStorageManager manager = context.getBean(FileStorageManager.class);
                    assertThat(manager).isNotNull();
                    // 云后端按桶名纳入统一管理器
                    assertThat(manager.getStorage("oss")).isNotNull();
                    assertThat(manager.getStorage("s3")).isNotNull();
                    assertThat(manager.getStorage("minio")).isNotNull();
                    // 默认桶始终为本地存储兜底
                    assertThat(manager.getStorage("default")).isInstanceOf(LocalFileStorage.class);
                });
    }

    /** 提供 OSS/S3/MINIO 类型的自定义存储 Bean，用于覆盖 bucketNameOf 的云分支（无需真实 SDK）。 */
    @Configuration(proxyBeanMethods = false)
    static class MultiStorageConfig {

        @Bean
        FileStorage ossStorage() {
            FileStorage s = mock(FileStorage.class);
            when(s.type()).thenReturn(StorageType.OSS);
            return s;
        }

        @Bean
        FileStorage s3Storage() {
            FileStorage s = mock(FileStorage.class);
            when(s.type()).thenReturn(StorageType.S3);
            return s;
        }

        @Bean
        FileStorage minioStorage() {
            FileStorage s = mock(FileStorage.class);
            when(s.type()).thenReturn(StorageType.MINIO);
            return s;
        }
    }
}
