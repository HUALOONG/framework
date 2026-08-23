package cn.jowen.framework.extras.storage.config;

import org.jspecify.annotations.NullMarked;

/**
 * MinIO 云存储配置 POJO。
 *
 * <p>对应属性前缀 {@code framework.extras.storage.minio}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class MinioProperties {

    /**
     * MinIO 服务器地址（如 http://localhost:9000）。
     */
    private String endpoint;

    /**
     * 默认 bucket 名称。
     */
    private String bucket;

    /**
     * Access Key。
     */
    private String accessKey;

    /**
     * Secret Key。
     */
    private String secretKey;

    /**
     * 区域（通常为空字符串）。
     */
    private String region = "";

    public MinioProperties() {
    }

    public MinioProperties(String endpoint, String bucket, String accessKey, String secretKey, String region) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
