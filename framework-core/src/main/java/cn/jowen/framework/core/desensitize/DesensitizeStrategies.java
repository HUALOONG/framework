package cn.jowen.framework.core.desensitize;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * 内置脱敏策略：按常见敏感数据类型预置保留位数与格式校验。
 *
 * <p>调用方式：
 * <pre>{@code
 * DesensitizeStrategies.PHONE.mask("13812345678");              // 138****5678
 * DesensitizeStrategies.ID_CARD.mask("11010519491231002X", DesensitizeContext.of(6, 4));
 * }</pre>
 *
 * <p>自定义策略可通过实现 {@link DesensitizeRule} 并注册到 {@link Desensitizer} 扩展。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum DesensitizeStrategies {

    /**
     * 手机号：11 位，1[3-9] 开头。
     */
    PHONE(3, 4, "^(1[3-9]\\d{9})$"),

    /**
     * 身份证号：18 位（含末位 X）。
     */
    ID_CARD(3, 4, "^\\d{17}[0-9Xx]$"),

    /**
     * 银行卡号：15-19 位数字。
     */
    BANK_CARD(4, 4, "^\\d{15,19}$"),

    /**
     * 邮箱：仅保留首字符与 @ 后域名。
     */
    EMAIL(1, 0, "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),

    /**
     * 中文姓名：仅保留姓氏。
     */
    NAME(1, 0, "^[\\u4e00-\\u9fa5]{2,4}$"),

    /**
     * 地址：保留前 3 个字符。
     */
    ADDRESS(3, 0, null),

    /**
     * 密码/口令：全量脱敏。
     */
    PASSWORD(0, 0, null),

    /**
     * 座机号码：区号 + 号码。
     */
    FIXED_PHONE(3, 4, "^0\\d{2,3}-?\\d{7,8}$"),

    /**
     * 车牌号：保留省份简称与首字符。
     */
    LICENSE_PLATE(3, 2, "^[\\u4e00-\\u9fa5][A-Za-z][A-Za-z0-9]{5,6}$"),

    /**
     * 自定义：默认全量脱敏，由 {@link DesensitizeContext} 覆盖。
     */
    CUSTOM(0, 0, null);

    private final int startKeep;
    private final int endKeep;
    private final @Nullable String regex;

    DesensitizeStrategies(int startKeep, int endKeep, @Nullable String regex) {
        this.startKeep = startKeep;
        this.endKeep = endKeep;
        this.regex = regex;
    }

    /**
     * 邮箱特殊处理：仅保留首字符与 @ 后域名。
     */
    private static String maskEmail(String raw, DesensitizeContext ctx) {
        int at = raw.indexOf('@');
        if (at <= 0) {
            return ctx.mask(raw);
        }
        String head = raw.charAt(0) + "***";
        return head + raw.substring(at);
    }

    /**
     * 策略默认开头保留位数。
     */
    public int defaultStartKeep() {
        return startKeep;
    }

    /**
     * 策略默认末尾保留位数。
     */
    public int defaultEndKeep() {
        return endKeep;
    }

    /**
     * 校验原始文本是否符合本策略格式（无正则约束的策略恒返回 true）。
     *
     * @param raw 原始文本
     * @return 匹配返回 true；null 或空白返回 false
     */
    public boolean matches(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        if (regex == null) {
            return true;
        }
        return Pattern.matches(regex, raw);
    }

    /**
     * 按策略默认上下文脱敏。
     *
     * @param raw 原始文本
     * @return 脱敏结果（null/空白原样返回）
     */
    public String mask(@Nullable String raw) {
        return mask(raw, null);
    }

    /**
     * 按给定上下文脱敏（上下文覆盖策略默认保留位/替换符）。
     *
     * <p>{@link DesensitizeContext#skip()} 为 true 时原样返回；
     * 格式校验失败（{@code regex} 非空且不匹配）时同样原样返回，避免误伤非法输入。
     *
     * @param raw 原始文本
     * @param ctx 执行上下文（null 时使用策略默认）
     * @return 脱敏结果
     */
    public String mask(@Nullable String raw, @Nullable DesensitizeContext ctx) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        DesensitizeContext effective = ctx != null ? ctx : new DesensitizeContext(startKeep, endKeep, "*", false);
        if (effective.skip()) {
            return raw;
        }
        if (regex != null && !matches(raw)) {
            return raw; // 格式不符，不脱敏
        }
        if (this == EMAIL) {
            return maskEmail(raw, effective);
        }
        return effective.mask(raw);
    }
}
