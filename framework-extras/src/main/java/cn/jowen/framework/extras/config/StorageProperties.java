package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 文件存储配置载体（轻量 POJO，非 Spring {@code @ConfigurationProperties}）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class StorageProperties {

    /**
     * 根目录（本地磁盘路径）。
     */
    private String rootLocation = "./storage";

    /**
     * 默认命名策略：date / hash / uuid / original。
     */
    private String namingStrategy = "date";

    /**
     * 是否生成预签名 URL（本地为 false）。
     */
    private boolean generatePresignedUrl = false;

    public StorageProperties() {
    }

    public String getRootLocation() {
        return rootLocation;
    }

    public void setRootLocation(String rootLocation) {
        this.rootLocation = rootLocation;
    }

    public String getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public boolean isGeneratePresignedUrl() {
        return generatePresignedUrl;
    }

    public void setGeneratePresignedUrl(boolean generatePresignedUrl) {
        this.generatePresignedUrl = generatePresignedUrl;
    }
}
