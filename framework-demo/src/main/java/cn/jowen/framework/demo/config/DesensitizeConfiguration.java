package cn.jowen.framework.demo.config;

import cn.jowen.framework.core.desensitize.DesensitizeRule;
import cn.jowen.framework.core.desensitize.DesensitizeStrategies;
import cn.jowen.framework.core.desensitize.Desensitizer;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 脱敏规则注册。
 *
 * <p><b>重要</b>：框架提供了 {@link Desensitizer} 门面与 {@link DesensitizeStrategies}
 * 内置策略常量，但<b>默认不注册任何 {@link DesensitizeRule}</b>——
 * {@code Desensitizer.getInstance().mask(text)} 仅在存在已注册规则时才会改写文本。
 * 这是框架刻意留出的扩展点：不同业务对"什么算敏感、如何打码"的诉求差异较大。
 *
 * <p>本类演示如何注册手机号与身份证两条规则，使日志中的敏感信息自动打码。
 *
 * <p><b>扩展提示</b>：如需邮箱、银行卡、地址等，照此增加规则即可；
 * 也可将规则实现为 SPI 扩展（标注 {@code @Activate}）供多个应用复用。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@Configuration
public class DesensitizeConfiguration {

    /** 手机号：1 开头 11 位，前后不为数字，避免误伤长数字串 */
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");

    /** 身份证：18 位，末位可为 X */
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{17}[0-9Xx])(?!\\d)");

    /**
     * 设置ister rules。
     */
    @PostConstruct
    public void registerRules() {
        Desensitizer.getInstance().register(phoneRule());
        Desensitizer.getInstance().register(idCardRule());
    }

    /** 手机号脱敏规则：13812345678 → 138****5678 */
    private static DesensitizeRule phoneRule() {
        return (text, ctx) -> PHONE.matcher(text).replaceAll(m ->
                Objects.requireNonNullElse(
                        DesensitizeStrategies.PHONE.mask(m.group(1)), m.group(1)));
    }

    /** 身份证脱敏规则：保留前 6 后 4 */
    private static DesensitizeRule idCardRule() {
        return (text, ctx) -> ID_CARD.matcher(text).replaceAll(m ->
                Objects.requireNonNullElse(
                        DesensitizeStrategies.ID_CARD.mask(m.group(1)), m.group(1)));
    }
}
