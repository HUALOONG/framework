package cn.jowen.framework.extras.storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件存储配置注解。
 *
 * <p>用于声明多存储后端 Bean，支持本地/MinIO/阿里云 OSS/AWS S3 等多种后端。
 *
 * <p>使用示例：
 * <pre>{@code
 * @StorageConfig(name = "minio", type = StorageType.MINIO)
 * @Bean
 * public FileStorage minioStorage(MinioProperties props) {
 *     return new MinioFileStorage(props);
 * }
 * }</pre>
 *
 * @author 王飞
 * @since 2026-08-25
 * @see FileStorageManager
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StorageConfig {

    /**
     * 存储后端名称，用于 {@link FileStorageManager#getStorage(String)} 查询。
     */
    String name();
}
