package cn.jowen.framework.plugin.dependency;

import org.jspecify.annotations.NullMarked;

/**
 * 依赖解析异常。在依赖缺失、版本不匹配或循环依赖时由 {@link DependencyResolver} 抛出。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class DependencyResolutionException extends RuntimeException {

    public DependencyResolutionException(String message) {
        super(message);
    }

    public DependencyResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
