package cn.jowen.framework.extras.common;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * 统一 API 响应体。
 *
 * <p>封装 {@code code}、{@code message} 与 {@code data}，便于前后端约定一致的返回结构。
 *
 * @param <T> 数据类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;
    private final @Nullable T data;

    private Result(int code, String message, @Nullable T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(@Nullable T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public @Nullable T getData() {
        return data;
    }

    public boolean isSuccess() {
        return code == 0;
    }
}
