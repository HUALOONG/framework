package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.FileStorageManager;
import cn.jowen.framework.extras.storage.local.LocalFileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link FileStorageAutoConfiguration} 装配方法体覆盖测试。
 *
 * <p>既有 {@code FileStorageAutoConfigurationTest} 走 {@code ApplicationContextRunner} 验证条件分支，
 * 但外层类构造器、{@code localFileStorage()} 与 {@code fileStorageManager()} 的方法体在条件
 * 不满足时不会被执行。本类直接实例化装配类，验证配置项到存储实现的构造透传与桶名映射。
 *
 * <p><b>为何不覆盖三个远程后端内部类</b>：{@code MinioConfiguration} / {@code OssConfiguration} /
 * {@code S3Configuration} 由 {@code @ConditionalOnClass}（字符串形式）保护，minio / aliyun-oss /
 * awssdk 均为 optional 依赖，本模块测试 classpath 中不存在，直接实例化会抛
 * {@code NoClassDefFoundError}，属结构性不可覆盖。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class FileStorageAutoConfigurationGapTest {

    /** 本地存储临时根目录。 */
    private static final String TMP_BASE =
            System.getProperty("java.io.tmpdir") + "/jowen-gap-local";

    @Test
    void localFileStorage_usesConfiguredBasePath() {
        BootStorageProperties props = new BootStorageProperties();
        props.setBasePath(TMP_BASE);

        assertThat(new FileStorageAutoConfiguration(props).localFileStorage())
                .isInstanceOf(LocalFileStorage.class);
    }

    @Test
    void fileStorageManager_registersEveryBackendUnderItsOwnBucket() {
        BootStorageProperties props = new BootStorageProperties();
        props.setBasePath(TMP_BASE);

        FileStorage local = storageOf(StorageType.LOCAL);
        FileStorage oss = storageOf(StorageType.OSS);
        FileStorage s3 = storageOf(StorageType.S3);
        FileStorage minio = storageOf(StorageType.MINIO);

        FileStorageManager manager = new FileStorageAutoConfiguration(props)
                .fileStorageManager(List.of(local, oss, s3, minio));

        assertThat(manager.getStorage("local")).isSameAs(local);
        assertThat(manager.getStorage("oss")).isSameAs(oss);
        assertThat(manager.getStorage("s3")).isSameAs(s3);
        assertThat(manager.getStorage("minio")).isSameAs(minio);
        assertThat(manager.getStorage("default")).isInstanceOf(LocalFileStorage.class);
    }

    @Test
    void fileStorageManager_withoutExtraStorages_stillProvidesDefaultLocalBucket() {
        BootStorageProperties props = new BootStorageProperties();
        props.setBasePath(TMP_BASE);

        FileStorageManager manager = new FileStorageAutoConfiguration(props)
                .fileStorageManager(List.of());

        assertThat(manager.getStorage("default")).isInstanceOf(LocalFileStorage.class);
    }

    private static FileStorage storageOf(StorageType type) {
        FileStorage storage = mock(FileStorage.class);
        when(storage.type()).thenReturn(type);
        return storage;
    }
}
