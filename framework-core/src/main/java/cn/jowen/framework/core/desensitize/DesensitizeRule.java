package cn.jowen.framework.core.desensitize;

import org.jspecify.annotations.NullMarked;

/**
 * 脱敏规则接口。实现类经 SPI 注册后由 {@link Desensitizer} 统一调度，作用于日志消息与结果集/JSON 输出。
 *
 * <p>规则在 {@link DesensitizeContext} 控制下对文本执行脱敏，上下文决定保留位数、替换符与跳过标记。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface DesensitizeRule {

    /**
     * 对输入文本执行脱敏，返回脱敏后文本。
     *
     * @param text 原始文本，不可为 {@code null}
     * @param ctx  执行上下文，决定保留位数/替换符/跳过标记，不可为 {@code null}
     * @return 脱敏后文本，不可为 {@code null}
     */
    String apply(String text, DesensitizeContext ctx);
}
