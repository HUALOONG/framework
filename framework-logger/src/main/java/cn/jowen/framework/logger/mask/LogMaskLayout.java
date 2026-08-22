package cn.jowen.framework.logger.mask;

import cn.jowen.framework.core.desensitize.Desensitizer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 日志布局装饰器，在最终输出文本上施加脱敏（适用于 Logback/Log4j2 Layout 包装）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class LogMaskLayout {

    private final Desensitizer desensitizer;

    public LogMaskLayout() {
        this(Desensitizer.getInstance());
    }

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
