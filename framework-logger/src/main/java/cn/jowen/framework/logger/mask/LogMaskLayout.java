package cn.jowen.framework.logger.mask;

import cn.jowen.framework.core.desensitize.Desensitizer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 日志布局装饰器，在最终输出文本上施加脱敏（适用于 Logback/Log4j2 Layout 包装）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LogMaskLayout {
    /**
     * 脱敏器，不可为 {@code null}
     */
    private final Desensitizer desensitizer;

    /**
     * 日志布局装饰器，默认使用 core 脱敏器。
     */
    public LogMaskLayout() {
        this(Desensitizer.getInstance());
    }

    /**
     * 日志布局装饰器，可指定脱敏器。
     *
     * @param desensitizer 脱敏器，不可为 {@code null}
     */
    public LogMaskLayout(Desensitizer desensitizer) {
        this.desensitizer = desensitizer;
    }

    /**
     * 对格式化后的日志文本脱敏。
     *
     * @param formatted 已格式化文本，可为 {@code null}
     * @return 脱敏后文本
     */
    public @Nullable String decorate(@Nullable String formatted) {
        if (formatted == null) {
            return null;
        }
        return desensitizer.mask(formatted);
    }
}
