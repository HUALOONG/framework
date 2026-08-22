package cn.jowen.framework.i18n.api;

import org.jspecify.annotations.NullMarked;

/**
 * 可热加载消息源。支持在运行时重新加载底层资源（如文件变更、DB 刷新），无需重启。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface ReloadableMessageSource extends MessageSource {

    /** 触发底层资源重新加载。 */
    void reload();
}
