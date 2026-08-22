package cn.jowen.framework.core.desensitize;

import org.jspecify.annotations.NullMarked;

/**
 * 脱敏规则接口。实现类经 SPI 注册后由 {@link Desensitizer} 统一调度，作用于日志消息与结果集/JSON 输出。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface DesensitizeRule {

    /**
     * 对输入文本执行脱敏，返回脱敏后文本。
     *
     * @param text 原始文本，不可为 {@code null}
     * @return 脱敏后文本，不可为 {@code null}
     */
    String apply(String text);
}
