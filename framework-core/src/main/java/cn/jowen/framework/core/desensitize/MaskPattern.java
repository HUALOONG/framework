package cn.jowen.framework.core.desensitize;

import org.jspecify.annotations.NullMarked;

import java.util.regex.Pattern;

/**
 * 内置掩码策略：基于正则匹配 + 局部遮蔽，覆盖手机号/身份证/银行卡/邮箱等常见敏感信息。
 *
 * <p>每个策略提供名称与正则；匹配到的分组中按 {@code keepHead}/{@code keepTail} 保留首尾若干字符，其余以 {@code maskChar} 替换。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class MaskPattern {

    private MaskPattern() {
    }

    /** 中国大陆手机号（独立 11 位数字串）：保留前 3 后 4。 */
    public static final Pattern PHONE = Pattern.compile("(?<![\\d])(1[3-9]\\d)(\\d{4})(\\d{4})(?![\\d])");

    /** 身份证号（独立 18 位数字串）：保留前 3 后 4。 */
    public static final Pattern ID_CARD = Pattern.compile("(?<![\\d])(\\d{3})\\d{11}(\\d{4})(?![\\d])");

    /** 银行卡号（独立 15-19 位数字串）：保留前 4 后 4。 */
    public static final Pattern BANK_CARD = Pattern.compile("(?<![\\d])(\\d{4})\\d{7,11}(\\d{4})(?![\\d])");

    /** 邮箱：保留 @ 前首字符与域名。 */
    public static final Pattern EMAIL = Pattern.compile("([a-zA-Z0-9])[a-zA-Z0-9._%+-]*(@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    /**
     * 对文本应用一组预定义策略的脱敏（顺序执行，重复执行幂等）。
     *
     * @param text 原文，可为 {@code null}
     * @return 脱敏后文本；{@code null} 原样返回
     */
    public static String maskAll(String text) {
        String result = text;
        result = replace(result, PHONE, "$1****$3");
        result = replace(result, ID_CARD, "$1********$2");
        result = replace(result, BANK_CARD, "$1****$2");
        result = replace(result, EMAIL, "$1***$2");
        return result;
    }

    private static String replace(String text, Pattern pattern, String replacement) {
        return pattern.matcher(text).replaceAll(replacement);
    }
}
