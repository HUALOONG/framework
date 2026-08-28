package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 对象存储命名策略枚举。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum NamingStrategy {

    /** 原始文件名 */
    ORIGINAL,

    /** UUID */
    UUID,

    /** 日期路径（yyyy/MM/dd） */
    DATE,

    /** 哈希（内容 MD5） */
    HASH,

    /** 自定义（由业务方通过 {@code ObjectNameStrategyFactory.register} 注册实现） */
    CUSTOM
}
