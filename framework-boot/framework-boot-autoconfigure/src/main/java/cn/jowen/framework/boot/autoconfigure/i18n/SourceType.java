package cn.jowen.framework.boot.autoconfigure.i18n;

import org.jspecify.annotations.NullMarked;

/**
 * 消息源类型。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public enum SourceType {

    /** classpath/文件系统 Properties 资源包。 */
    PROPERTIES,

    /** 数据库消息表。 */
    DATABASE
}
