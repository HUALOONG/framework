package cn.jowen.framework.i18n.support;

import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 区域匹配器：在支持的区域集合中为请求区域寻找最佳候选，
 * 供 Accept-Language 解析与资源包候选选择复用。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class LocaleMatcher {

    /** 默认区域（系统默认）。 */
    public static final Locale DEFAULT = Locale.getDefault();

    private LocaleMatcher() {
    }

    /**
     * 最佳匹配：精确（语言+国家）→ 语言 → 首个支持区域。
     * 注意：不依赖系统默认区域（结果可预测、可测试）。
     *
     * @param requested 请求区域，可为 {@code null}（视为默认）
     * @param supported 支持的区域集合，不可为 {@code null}
     * @return 最佳候选；集合为空返回 {@code null}
     */
    public static @Nullable Locale bestMatch(@Nullable Locale requested, List<Locale> supported) {
        if (supported.isEmpty()) {
            return null;
        }
        Locale req = requested != null ? requested : DEFAULT;
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

    /**
     * 精确匹配判断（语言+国家均一致）。
     *
     * @param a 区域，可为 {@code null}
     * @param b 区域，可为 {@code null}
     * @return 一致返回 {@code true}
     */
    public static boolean exact(@Nullable Locale a, @Nullable Locale b) {
        return a != null && b != null && a.getLanguage().equals(b.getLanguage())
                && a.getCountry().equalsIgnoreCase(b.getCountry());
    }

    /**
     * 语言级别匹配判断（仅语言一致）。
     *
     * @param a 区域，可为 {@code null}
     * @param b 区域，可为 {@code null}
     * @return 一致返回 {@code true}
     */
    public static boolean language(@Nullable Locale a, @Nullable Locale b) {
        return a != null && b != null && a.getLanguage().equals(b.getLanguage());
    }
}
