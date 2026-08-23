package cn.jowen.framework.plugin.resolver;

/**
 * 依赖解析异常。
 *
 * @author 王飞
 */
public final class DependencyResolutionException extends RuntimeException {
    public DependencyResolutionException(String message) {
        super(message);
    }

    public DependencyResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
