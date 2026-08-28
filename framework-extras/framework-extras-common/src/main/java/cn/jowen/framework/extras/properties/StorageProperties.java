package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 文件存储配置。
 *
 * <p>存储类型复用 common 内的 {@link StorageType}（全框架唯一权威枚举），
 * 消除此前 {@code StorageProperties.StorageType} 与 {@code storage.StorageType}
 * 并存造成的双枚举冲突。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class StorageProperties {

    /** enabled 字段。 */
    private boolean enabled = true;
    /** type 字段。 */
    private StorageType type = StorageType.LOCAL;
    /** namingStrategy 字段。 */
    private NamingStrategy namingStrategy = NamingStrategy.UUID;
    /** basePath 字段。 */
    private String basePath = "/tmp/framework-extras-storage";
    /** endpoint 字段。 */
    private String endpoint = "";
    /** accessKey 字段。 */
    private String accessKey = "";
    /** secretKey 字段。 */
    private String secretKey = "";
    /** bucket 字段。 */
    private String bucket = "";
    /** region 字段。 */
    private String region = "";

    /**
     * 获取enabled。
     * @return 结果
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置enabled。
     * @param enabled 参数 enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取type。
     * @return 结果
     */
    public StorageType getType() {
        return type;
    }

    /**
     * 设置type。
     * @param type 参数 type
     */
    public void setType(StorageType type) {
        this.type = type;
    }

    /**
     * 获取naming strategy。
     * @return 结果
     */
    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    /**
     * 设置naming strategy。
     * @param namingStrategy 参数 namingStrategy
     */
    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    /**
     * 获取base path。
     * @return 结果
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * 设置base path。
     * @param basePath 参数 basePath
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    /**
     * 获取endpoint。
     * @return 结果
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * 设置endpoint。
     * @param endpoint 参数 endpoint
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * 获取access key。
     * @return 结果
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * 设置access key。
     * @param accessKey 参数 accessKey
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * 获取secret key。
     * @return 结果
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * 设置secret key。
     * @param secretKey 参数 secretKey
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * 获取bucket。
     * @return 结果
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * 设置bucket。
     * @param bucket 参数 bucket
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * 获取region。
     * @return 结果
     */
    public String getRegion() {
        return region;
    }

    /**
     * 设置region。
     * @param region 参数 region
     */
    public void setRegion(String region) {
        this.region = region;
    }
}
