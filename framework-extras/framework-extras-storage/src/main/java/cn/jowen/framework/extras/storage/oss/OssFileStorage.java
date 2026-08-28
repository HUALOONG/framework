package cn.jowen.framework.extras.storage.oss;

import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.exception.StorageException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 基于阿里云 OSS 的 {@link FileStorage} 实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class OssFileStorage implements FileStorage {

    private final OSS ossClient;
    private final String bucketName;
    private final String endpoint;

    public OssFileStorage(String endpoint, String bucketName, String accessKey, String secretKey) {
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
    }

    @Override
    public StorageType type() {
        return StorageType.OSS;
    }

    @Override
    public String put(String key, InputStream content, @Nullable String contentType) {
        try {
            com.aliyun.oss.model.PutObjectRequest request =
                    new com.aliyun.oss.model.PutObjectRequest(bucketName, key, content);
            if (contentType != null) {
                request.setMetadata(new com.aliyun.oss.model.ObjectMetadata() {{
                    setContentType(contentType);
                }});
            }
            ossClient.putObject(request);
            return key;
        } catch (Exception e) {
            throw new StorageException("OSS 上传失败: " + key, e);
        }
    }

    @Override
    public @Nullable InputStream get(String key) {
        try {
            if (!exists(key)) {
                return null;
            }
            OSSObject object = ossClient.getObject(bucketName, key);
            return object.getObjectContent();
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("OSS 下载失败: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            throw new StorageException("OSS 删除失败: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return ossClient.doesObjectExist(bucketName, key);
        } catch (Exception e) {
            throw new StorageException("OSS 存在性判断失败: " + key, e);
        }
    }

    @Override
    public URL generateUrl(String key, long expire) {
        try {
            long millis = expire > 0 ? expire : 60 * 60 * 1000L;
            return ossClient.generatePresignedUrl(bucketName, key, new Date(System.currentTimeMillis() + millis));
        } catch (Exception e) {
            throw new StorageException("OSS 生成 URL 失败: " + key, e);
        }
    }
}
