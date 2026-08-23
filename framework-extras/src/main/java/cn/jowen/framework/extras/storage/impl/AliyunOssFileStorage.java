package cn.jowen.framework.extras.storage.impl;

import cn.jowen.framework.extras.storage.FileInfo;
import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.StorageException;
import cn.jowen.framework.extras.storage.config.AliyunOssProperties;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.HeadObjectRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 阿里云 OSS 云存储实现。
 *
 * <p>基于 {@code com.aliyun.oss:aliyun-sdk-oss} SDK；依赖 optional。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class AliyunOssFileStorage implements FileStorage {

    private final AliyunOssProperties props;
    private final OSS ossClient;

    public AliyunOssFileStorage(AliyunOssProperties props) {
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.ossClient = new OSSClientBuilder().build(
                props.getEndpoint(),
                props.getAccessKeyId(),
                props.getAccessKeySecret());
    }

    @Override
    public FileInfo upload(InputStream in, String originalFilename) {
        Objects.requireNonNull(in, "input stream must not be null");
        String objectName = sanitizeObjectName(originalFilename);
        try {
            PutObjectRequest putRequest = new PutObjectRequest(props.getBucket(), objectName, in);
            PutObjectResult result = ossClient.putObject(putRequest);
            return new FileInfo(objectName, originalFilename, null, 0,
                    result.getETag(), 0, buildUrl(objectName), null);
        } catch (OSSException e) {
            throw new StorageException("OSS 上传失败: " + objectName + " code=" + e.getErrorCode(), e);
        } catch (Exception e) {
            throw new StorageException("OSS 上传失败: " + objectName, e);
        }
    }

    @Override
    public InputStream download(String objectName) {
        try {
            OSSObject ossObject = ossClient.getObject(props.getBucket(), objectName);
            return ossObject.getObjectContent();
        } catch (OSSException e) {
            throw new StorageException("OSS 下载失败: " + objectName + " code=" + e.getErrorCode(), e);
        } catch (Exception e) {
            throw new StorageException("OSS 下载失败: " + objectName, e);
        }
    }

    @Override
    public @Nullable String getPresignedUrl(String objectName) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + 3600L * 1000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    props.getBucket(), objectName, HttpMethod.GET);
            request.setExpiration(expiration);
            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } catch (OSSException e) {
            throw new StorageException("OSS 生成预签名 URL 失败: " + objectName + " code=" + e.getErrorCode(), e);
        } catch (Exception e) {
            throw new StorageException("OSS 生成预签名 URL 失败: " + objectName, e);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            ossClient.deleteObject(props.getBucket(), objectName);
        } catch (OSSException e) {
            throw new StorageException("OSS 删除失败: " + objectName + " code=" + e.getErrorCode(), e);
        } catch (Exception e) {
            throw new StorageException("OSS 删除失败: " + objectName, e);
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
            ossClient.headObject(new HeadObjectRequest(props.getBucket(), objectName));
            return true;
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                return false;
            }
            throw new StorageException("OSS 检查对象存在性失败: " + objectName, e);
        } catch (Exception e) {
            throw new StorageException("OSS 检查对象存在性失败: " + objectName, e);
        }
    }

    @Override
    public @Nullable FileInfo getFileInfo(String objectName) {
        try {
            ObjectMetadata metadata = ossClient.headObject(new HeadObjectRequest(props.getBucket(), objectName));
            return new FileInfo(objectName, objectName, metadata.getContentType(),
                    metadata.getContentLength(), metadata.getETag(),
                    metadata.getLastModified().getTime(), buildUrl(objectName), null);
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                return null;
            }
            throw new StorageException("OSS 获取元信息失败: " + objectName, e);
        } catch (Exception e) {
            throw new StorageException("OSS 获取元信息失败: " + objectName, e);
        }
    }

    @Override
    public List<FileInfo> listObjects(@Nullable String prefix) {
        List<FileInfo> result = new ArrayList<>();
        try {
            ListObjectsRequest req = new ListObjectsRequest();
            req.setBucketName(props.getBucket());
            req.setPrefix(prefix == null ? "" : prefix);
            ObjectListing listing = ossClient.listObjects(req);
            for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                result.add(new FileInfo(summary.getKey(), summary.getKey(),
                        summary.getType(), summary.getSize(),
                        summary.getETag(), summary.getLastModified().getTime(),
                        buildUrl(summary.getKey()), null));
            }
        } catch (OSSException e) {
            throw new StorageException("OSS 列举对象失败", e);
        } catch (Exception e) {
            throw new StorageException("OSS 列举对象失败", e);
        }
        return result;
    }

    private String buildUrl(String objectName) {
        String endpoint = props.getEndpoint();
        String bucket = props.getBucket();
        if (endpoint == null || bucket == null) {
            return null;
        }
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/" + bucket + "/" + objectName;
    }

    private String sanitizeObjectName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload-" + System.currentTimeMillis();
        }
        int lastSlash = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? originalFilename.substring(lastSlash + 1) : originalFilename;
        return name.isBlank() ? "upload-" + System.currentTimeMillis() : name;
    }
}
