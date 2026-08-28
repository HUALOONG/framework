package cn.jowen.framework.i18n.config;

import org.jspecify.annotations.NullMarked;

/**
 * 消息源类型。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum SourceType {

    /**
     * classpath/文件系统 Properties 资源包。
     */
    PROPERTIES,

    /**
     * 数据库消息表。
     */
    DATABASE,

    /**
     * Redis 消息缓存（骨架预留，实现依赖 cache 模块）。
     */
    REDIS,

    /**
     * 多源组合（按 {@code composite-order} 依次查找）。
     */
    COMPOSITE
}
