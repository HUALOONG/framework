package cn.jowen.framework.extras.storage.minio;

import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.FileStorage;
// 注：MinioFileStorage.type() 返回 common 权威 StorageType.MINIO
import cn.jowen.framework.extras.storage.exception.StorageException;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * 基于 MinIO 的 {@link FileStorage} 实现（兼容 S3 协议的开源对象存储）。
 *
 * <p>依赖 {@code io.minio:minio} 为可选依赖，未引入时本类不会加载。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class MinioFileStorage implements FileStorage {

    /** client 不可变字段。 */
    private final MinioClient client;
    /** bucketName 不可变字段。 */
    private final String bucketName;

    /**
     * 构造实例。
     * @param client 参数 client
     * @param bucketName 参数 bucketName
     */
    public MinioFileStorage(MinioClient client, String bucketName) {
        this.client = client;
        this.bucketName = bucketName;
    }

    /**
     * 便捷构造：按 endpoint / ak / sk 构建客户端。
     *
     * @param endpoint   MinIO 服务地址
     * @param accessKey  访问密钥
     * @param secretKey  密钥
     * @param bucketName 桶名
     */
    public MinioFileStorage(String endpoint, String accessKey, String secretKey, String bucketName) {
        this(MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build(), bucketName);
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public StorageType type() {
        return StorageType.MINIO;
    }

    /**
     * 设置。
     * @param key 参数 key
     * @param content 参数 content
     * @param contentType 参数 contentType
     * @return 结果
     */
    @Override
    public String put(String key, InputStream content, @Nullable String contentType) {
        try {
            byte[] bytes = content.readAllBytes();
            var builder = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L);
            if (contentType != null) {
                builder.contentType(contentType);
            }
            client.putObject(builder.build());
            return key;
        } catch (Exception e) {
            throw new StorageException("MinIO 上传失败: " + key, e);
        }
    }

    /**
     * 获取。
     * @param key 参数 key
     * @return 结果
     */
    @Override
    public @Nullable InputStream get(String key) {
        try {
            if (!exists(key)) {
                return null;
            }
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucketName).object(key).build());
        } catch (Exception e) {
            throw new StorageException("MinIO 下载失败: " + key, e);
        }
    }

    /**
     * 执行delete操作。
     * @param key 参数 key
     */
    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName).object(key).build());
        } catch (Exception e) {
            throw new StorageException("MinIO 删除失败: " + key, e);
        }
    }

    /**
     * 执行exists操作。
     * @param key 参数 key
     * @return 结果
     */
    @Override
    public boolean exists(String key) {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(bucketName).object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new StorageException("MinIO 存在性判断失败: " + key, e);
        }
    }

    /**
     * 执行generate url操作。
     * @param key 参数 key
     * @param expire 参数 expire
     * @return 结果
     */
    @Override
    public URL generateUrl(String key, long expire) {
        try {
            int seconds = (int) (expire > 0 ? TimeUnit.MILLISECONDS.toSeconds(expire) : 3600L);
            return new URL(client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.Http.Method.GET)
                    .bucket(bucketName)
                    .object(key)
                    .expiry(seconds)
                    .build()));
        } catch (Exception e) {
            throw new StorageException("MinIO 生成 URL 失败: " + key, e);
        }
    }
}
