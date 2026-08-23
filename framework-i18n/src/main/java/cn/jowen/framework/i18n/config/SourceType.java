package cn.jowen.framework.i18n.config;

import org.jspecify.annotations.NullMarked;

/**
 * 消息源类型。
 *
 * @author 王飞
 * @since 2026-08-24
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
    DATABASE
}
