package cn.jowen.framework.i18n.api;

import org.jspecify.annotations.NullMarked;

/**
 * 资源加载异常。底层资源（properties 文件 / 数据库 / Redis）读取、解析失败时抛出，
 * 与 {@code ResourceLoadFailedEvent} 互为补充：本异常面向同步调用方，事件面向异步监控。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ResourceLoadException extends I18nException {

    public ResourceLoadException(String message) {
        super(I18nErrorCode.RESOURCE_LOAD_FAILED, message);
    }

    public ResourceLoadException(String message, Throwable cause) {
        super(I18nErrorCode.RESOURCE_LOAD_FAILED, message, cause);
    }
}
