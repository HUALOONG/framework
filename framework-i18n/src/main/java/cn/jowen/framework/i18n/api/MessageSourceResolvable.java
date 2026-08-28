package cn.jowen.framework.i18n.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 可解析消息引用：编码 + 格式化参数 + 默认文案。用于延迟到解析时才确定语言环境的消息引用
 * （如异常消息、列表页文案），与 {@link MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)}
 * 配合使用。
 *
 * @param code           消息编码，不可为 {@code null}
 * @param args           格式化参数，可为 {@code null}
 * @param defaultMessage 未找到编码时使用的默认文案，可为 {@code null}
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record MessageSourceResolvable(String code, @Nullable Object @Nullable [] args,
                                      @Nullable String defaultMessage) {

    public MessageSourceResolvable {
        Objects.requireNonNull(code, "code must not be null");
    }

    /**
     * 便捷构造：仅编码。
     */
    public MessageSourceResolvable(String code) {
        this(code, null, null);
    }

    /**
     * 便捷构造：编码 + 默认文案。
     */
    public MessageSourceResolvable(String code, @Nullable String defaultMessage) {
        this(code, null, defaultMessage);
    }
}
