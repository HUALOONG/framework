package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.FileStorageManager;
import cn.jowen.framework.extras.storage.local.LocalFileStorage;
import cn.jowen.framework.extras.storage.minio.MinioFileStorage;
import cn.jowen.framework.extras.storage.oss.OssFileStorage;
import cn.jowen.framework.extras.storage.s3.S3Clients;
import org.jspecify.annotations.NullMarked;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储模块自动装配。
 *
 * <p>始终注册本地磁盘存储作为默认桶；用户可注入更多 {@link FileStorage} Bean
 * （OSS / S3 / Minio 等），本装配会将其按桶名纳入 {@link FileStorageManager} 统一管理。
 *
 * <p>各后端分别由独立的内部配置类承载，通过 {@link ConditionalOnClass} +
 * {@code framework.extras.storage.type} 双重条件激活，确保未引入对应 SDK
 * 或未按类型配置时不会产生类加载失败。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BootStorageProperties.class)
public class FileStorageAutoConfiguration {

    /** DEFAULT_BUCKET 常量。 */
    private static final String DEFAULT_BUCKET = "default";

    /** props 不可变字段。 */
    private final BootStorageProperties props;

    /**
     * 构造实例。
     * @param props 参数 props
     */
    public FileStorageAutoConfiguration(BootStorageProperties props) {
        this.props = props;
    }

    /**
     * 执行local file storage操作。
     * @return 结果
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalFileStorage localFileStorage() {
        return new LocalFileStorage(props.getBasePath());
    }

    /**
     * 执行file storage manager操作。
     * @return 结果
     */
    @Bean
    @ConditionalOnMissingBean
    public FileStorageManager fileStorageManager(List<FileStorage> storages) {
        Map<String, FileStorage> map = new HashMap<>();
        for (FileStorage storage : storages) {
            map.put(bucketNameOf(storage), storage);
        }
        map.putIfAbsent(DEFAULT_BUCKET, new LocalFileStorage(props.getBasePath()));
        return new FileStorageManager(map, DEFAULT_BUCKET);
    }

    private static String bucketNameOf(FileStorage storage) {
        return switch (storage.type()) {
            case LOCAL -> "local";
            case OSS -> "oss";
            case S3 -> "s3";
            case MINIO -> "minio";
        };
    }

    /**
     * MinIO 装配：仅当 classpath 存在 MinioClient 且配置 {@code storage.type=MINIO} 时生效。
     *
     * <p>独立内部类可避免未引入 MinIO SDK 时因类加载失败导致整体装配不可用。
     */
    @NullMarked
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.minio.MinioClient")
    @ConditionalOnProperty(prefix = "framework.extras.storage", name = "type", havingValue = "MINIO")
    public static class MinioConfiguration {

        /** props 不可变字段。 */
        private final BootStorageProperties props;

        /**
         * 构造实例。
         * @param props 参数 props
         */
        public MinioConfiguration(BootStorageProperties props) {
            this.props = props;
        }

        /**
         * 执行minio file storage操作。
         * @return 结果
         */
        @Bean
        @ConditionalOnMissingBean
        public FileStorage minioFileStorage() {
            return new MinioFileStorage(
                    props.getEndpoint(), props.getAccessKey(), props.getSecretKey(), props.getBucket());
        }
    }

    /**
     * 阿里云 OSS 装配：仅当 classpath 存在 OSSClientBuilder 且配置 {@code storage.type=OSS} 时生效。
     */
    @NullMarked
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.aliyun.oss.OSSClientBuilder")
    @ConditionalOnProperty(prefix = "framework.extras.storage", name = "type", havingValue = "OSS")
    public static class OssConfiguration {

        /** props 不可变字段。 */
        private final BootStorageProperties props;

        /**
         * 构造实例。
         * @param props 参数 props
         */
        public OssConfiguration(BootStorageProperties props) {
            this.props = props;
        }

        /**
         * 执行oss file storage操作。
         * @return 结果
         */
        @Bean
        @ConditionalOnMissingBean
        public FileStorage ossFileStorage() {
            return new OssFileStorage(
                    props.getEndpoint(), props.getBucket(), props.getAccessKey(), props.getSecretKey());
        }
    }

    /**
     * AWS S3 装配：仅当 classpath 存在 S3Client 且配置 {@code storage.type=S3} 时生效。
     */
    @NullMarked
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "software.amazon.awssdk.services.s3.S3Client")
    @ConditionalOnProperty(prefix = "framework.extras.storage", name = "type", havingValue = "S3")
    public static class S3Configuration {

        /** props 不可变字段。 */
        private final BootStorageProperties props;

        /**
         * 构造实例。
         * @param props 参数 props
         */
        public S3Configuration(BootStorageProperties props) {
            this.props = props;
        }

        /**
         * 执行s3 file storage操作。
         * @return 结果
         */
        @Bean
        @ConditionalOnMissingBean
        public FileStorage s3FileStorage() {
            // SDK 客户端构建细节封闭在 storage 模块的 S3Clients 内，装配层不引入 AWS SDK
            return S3Clients.create(props.getRegion(), props.getAccessKey(),
                    props.getSecretKey(), props.getBucket());
        }
    }
}
