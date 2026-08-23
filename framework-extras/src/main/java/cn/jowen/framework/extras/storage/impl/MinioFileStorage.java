package cn.jowen.framework.extras.storage.impl;

import cn.jowen.framework.extras.storage.FileInfo;
import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.StorageException;
import cn.jowen.framework.extras.storage.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.Http.Method;
import io.minio.messages.Item;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 云存储实现。
 *
 * <p>基于 {@code io.minio:minio} SDK；依赖 optional，未引入时由
 * {@code @ConditionalOnClass} 保证不注册。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class MinioFileStorage implements FileStorage {

    private final MinioProperties props;
    private final MinioClient client;

    public MinioFileStorage(MinioProperties props) {
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.client = buildClient(props);
        ensureBucketExists(props.getBucket());
    }

    private static MinioClient buildClient(MinioProperties props) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey());
        if (!props.getRegion().isBlank()) {
            builder.region(props.getRegion());
        }
        return builder.build();
    }

    @Override
    public FileInfo upload(InputStream in, String originalFilename) {
        Objects.requireNonNull(in, "input stream must not be null");
        String objectName = sanitizeObjectName(originalFilename);
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .stream(in, (long) -1, (long) (10 * 1024 * 1024))
                    .build());
        } catch (Exception e) {
            throw new StorageException("MinIO 上传失败: " + objectName, e);
        }
        return new FileInfo(objectName, originalFilename, null, 0, null, 0,
                buildBaseUrl(objectName), null);
    }

    @Override
    public InputStream download(String objectName) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new StorageException("MinIO 下载失败: " + objectName, e);
        }
    }

    @Override
    public @Nullable String getPresignedUrl(String objectName) {
        try {
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(props.getBucket())
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            throw new StorageException("MinIO 生成预签名 URL 失败: " + objectName, e);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new StorageException("MinIO 删除失败: " + objectName, e);
        }
    }

    @Override
    public void deleteBatch(List<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            delete(name);
        }
    }

    @Override
    public boolean exists(String objectName) {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public @Nullable FileInfo getFileInfo(String objectName) {
        try {
            io.minio.StatObjectResponse stat = client.statObject(StatObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .build());
            return new FileInfo(objectName, objectName, stat.contentType(),
                    stat.size(), stat.etag(), stat.lastModified().toInstant().toEpochMilli(),
                    buildBaseUrl(objectName), null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<FileInfo> listObjects(@Nullable String prefix) {
        List<FileInfo> result = new ArrayList<>();
        try {
            for (io.minio.Result<Item> r : client.listObjects(ListObjectsArgs.builder()
                    .bucket(props.getBucket())
                    .prefix(prefix == null ? "" : prefix)
                    .recursive(true)
                    .build())) {
                Item item = r.get();
                result.add(new FileInfo(item.objectName(), item.objectName(),
                        item.storageClass(), item.size(), item.etag(),
                        item.lastModified().toInstant().toEpochMilli(),
                        buildBaseUrl(item.objectName()), null));
            }
        } catch (Exception e) {
            throw new StorageException("MinIO 列举对象失败", e);
        }
        return result;
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new StorageException("MinIO bucket 初始化失败: " + bucket, e);
        }
    }

    private String buildBaseUrl(String objectName) {
        String endpoint = props.getEndpoint();
        if (endpoint == null) {
            return null;
        }
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/" + props.getBucket() + "/" + objectName;
    }

    private String sanitizeObjectName(String originalFilename) {
        if (originalFilename.isBlank()) {
            return "upload-" + System.currentTimeMillis();
        }
        int lastSlash = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? originalFilename.substring(lastSlash + 1) : originalFilename;
        if (name.isBlank()) {
            return "upload-" + System.currentTimeMillis();
        }
        return name;
    }
}
