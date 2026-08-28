package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.properties.StorageProperties;
import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.FileStorageManager;
import cn.jowen.framework.extras.storage.local.LocalFileStorage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
}
