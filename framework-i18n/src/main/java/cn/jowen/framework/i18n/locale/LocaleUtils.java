package cn.jowen.framework.i18n.locale;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 区域工具：Accept-Language 头解析、语言标签规范化、区域匹配。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class LocaleUtils {

    private LocaleUtils() {
    }

    /**
     * 解析 Accept-Language 头（RFC 7231），按质量值（q）降序返回。
     * 非法片段静默忽略；q=0 片段被排除；未携带质量的片段视为 q=1。
     *
     * @param header Accept-Language 值，可为 {@code null}
     * @return 区域列表（按优先级降序），永不为 {@code null}
     */
    public static List<Locale> parseAcceptLanguage(@Nullable String header) {
        List<Locale> result = new ArrayList<>();
        if (header == null || header.isBlank()) {
            return result;
        }
        List<WeightedLocale> weighted = new ArrayList<>();
        for (String part : header.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] pieces = trimmed.split(";");
            String tag = pieces[0].trim();
            double q = 1.0;
            for (int i = 1; i < pieces.length; i++) {
                String param = pieces[i].trim();
                if (param.startsWith("q=")) {
                    try {
                        q = Double.parseDouble(param.substring(2).trim());
                    } catch (NumberFormatException ignored) {
                        q = 0.0;
                    }
                }
            }
            if (q <= 0.0 || tag.equals("*")) {
                continue;
            }
            Locale locale = parseTag(tag);
            if (locale != null) {
                weighted.add(new WeightedLocale(locale, q));
            }
        }
        weighted.sort(Comparator.comparingDouble((WeightedLocale w) -> w.q).reversed());
        for (WeightedLocale w : weighted) {
            result.add(w.locale);
        }
        return result;
    }

    /**
     * 解析 BCP 47 语言标签（如 {@code zh-CN}、{@code en}、{@code zh-Hans-CN}）。
     * 脚本部分（{@code -Hans}）被忽略；非法标签返回 {@code null}。
     *
     * @param tag 语言标签，可为 {@code null}
     * @return 区域；非法返回 {@code null}
     */
    public static @Nullable Locale parseTag(@Nullable String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        String[] parts = tag.split("-");
        String language = parts[0].toLowerCase();
        if (language.isBlank()) {
            return null;
        }
        // 提取国家/地区（两字母大写），忽略脚本
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() == 2 || part.length() == 3) {
                return new Locale(language, part.toUpperCase());
            }
        }
        return new Locale(language);
    }

    /**
     * 规范化语言标签（{@code zh-CN} → {@code zh_CN}、{@code EN} → {@code en}）。
     *
     * @param tag 语言标签，可为 {@code null}
     * @return 规范化后的标签；非法输入返回原值
     */
    public static String normalize(@Nullable String tag) {
        Locale locale = parseTag(tag);
        return locale != null ? locale.toString() : (tag != null ? tag : "");
    }

    /**
     * 在两个区域间进行最接近匹配：先精确匹配（语言+国家），再语言匹配，最后回退到首个支持区域。
     * 注意：不依赖系统默认区域（结果可预测、可测试）。
     *
     * @param requested 请求区域，可为 {@code null}（视为默认）
     * @param supported 支持的区域集合，不可为 {@code null}
     * @return 最佳匹配区域；支持集合为空返回 {@code null}
     */
    public static @Nullable Locale match(@Nullable Locale requested, List<Locale> supported) {
        if (supported.isEmpty()) {
            return null;
        }
        Locale req = requested != null ? requested : Locale.getDefault();
        for (Locale locale : supported) {
            if (locale.getLanguage().equals(req.getLanguage())
                    && locale.getCountry().equalsIgnoreCase(req.getCountry())) {
                return locale;
            }
        }
        for (Locale locale : supported) {
            if (locale.getLanguage().equals(req.getLanguage())) {
                return locale;
            }
        }
        return supported.get(0);
    }

    private record WeightedLocale(Locale locale, double q) {
    }
}
