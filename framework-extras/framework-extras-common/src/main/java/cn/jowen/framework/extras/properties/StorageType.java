package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 存储类型枚举（全框架唯一权威定义）。
 *
 * <p>置于 common（L0）以避免 storage 模块被 common 反向依赖；
 * storage 与各后端实现均引用本枚举。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum StorageType {
    /** 本地磁盘 */
    LOCAL,
    /** 阿里云 OSS */
    OSS,
    /** AWS S3 */
    S3,
    /** MinIO（兼容 S3 协议的开源对象存储） */
    MINIO
}
