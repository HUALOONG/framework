package cn.jowen.framework.extras.storage.config;

import org.jspecify.annotations.NullMarked;

/**
 * AWS S3 云存储配置 POJO（兼容 MinIO 协议端点）。
 *
 * <p>对应属性前缀 {@code framework.extras.storage.aws-s3}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class AwsS3Properties {

    /**
     * AWS 区域（如 us-east-1）。
     */
    private String region;

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
     * 自定义 Endpoint（可选，用于 S3 兼容服务如 MinIO）。
     */
    private String endpoint;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
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

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
