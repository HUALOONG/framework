package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * IP 属地解析配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class Ip2RegionProperties {

    /** enabled 字段。 */
    private boolean enabled = false;
    /** loadType 字段。 */
    private LoadType loadType = LoadType.MEMORY;
    /** dbPath 字段。 */
    private String dbPath = "ip2region.xdb";

    /** 库加载方式 */
    public enum LoadType {
        /** 内存加载：全量载入 xdb，查询最快 */
        MEMORY,
        /** 索引加载：仅载入索引，查询时读文件，内存占用低 */
        INDEX,
        /** 文件映射：基于文件随机访问，内存占用最低 */
        FILE
    }

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
     * 获取load type。
     * @return 结果
     */
    public LoadType getLoadType() {
        return loadType;
    }

    /**
     * 设置load type。
     * @param loadType 参数 loadType
     */
    public void setLoadType(LoadType loadType) {
        this.loadType = loadType;
    }

    /**
     * 获取db path。
     * @return 结果
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * 设置db path。
     * @param dbPath 参数 dbPath
     */
    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }
}
