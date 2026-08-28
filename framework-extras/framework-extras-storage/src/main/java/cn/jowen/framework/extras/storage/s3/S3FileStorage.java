package cn.jowen.framework.extras.storage.s3;

import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.exception.StorageException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

/**
 * 基于 AWS S3 的 {@link FileStorage} 实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucketName;

    public S3FileStorage(S3Client s3Client, S3Presigner presigner, String bucketName) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucketName = bucketName;
    }

    @Override
    public StorageType type() {
        return StorageType.S3;
    }

    @Override
    public String put(String key, InputStream content, @Nullable String contentType) {
        try {
            byte[] bytes = content.readAllBytes();
            var builder = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key);
            if (contentType != null) {
                builder.contentType(contentType);
            }
            s3Client.putObject(builder.build(), RequestBody.fromBytes(bytes));
            return key;
        } catch (Exception e) {
            throw new StorageException("S3 上传失败: " + key, e);
        }
    }

    @Override
    public @Nullable InputStream get(String key) {
        try {
            if (!exists(key)) {
                return null;
            }
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucketName).key(key).build());
            return new ByteArrayInputStream(bytes.asByteArray());
        } catch (S3Exception e) {
            throw new StorageException("S3 下载失败: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                    .bucket(bucketName).key(key).build());
        } catch (S3Exception e) {
            throw new StorageException("S3 删除失败: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                    .bucket(bucketName).key(key).build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new StorageException("S3 存在性判断失败: " + key, e);
        }
    }

    @Override
    public URL generateUrl(String key, long expire) {
        try {
            long millis = expire > 0 ? expire : 60 * 60 * 1000L;
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName).key(key).build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMillis(millis))
                    .getObjectRequest(getRequest)
                    .build();
            PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
            return presigned.url();
        } catch (Exception e) {
            throw new StorageException("S3 生成 URL 失败: " + key, e);
        }
    }
}
