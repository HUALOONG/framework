package cn.jowen.framework.i18n.source;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * 基于 JDK {@link ResourceBundle} 的消息源适配：按 basename 定位资源包
 * （如 {@code i18n.messages_zh_CN.properties}），复用 JDK 的候选回退机制。
 *
 * <p>与裸 {@link ResourceBundle} 的区别（自定义 {@link Control}）：
 * <ul>
 *   <li>候选链仅 {@code [请求区域, 语言, ROOT]}，不插入系统默认区域（避免 zh 环境误中 zh 包）；</li>
 *   <li>{@link Control#getFallbackLocale} 返回 {@code null}，禁止 JDK 在 ROOT 命中后
 *       继续回退到系统默认区域（否则 en 请求会误加载 zh_CN 包并覆盖 ROOT 文案）；</li>
 *   <li>资源包按 UTF-8 读取（JDK 默认 ISO-8859-1 会导致中文乱码）。</li>
 * </ul>
 *
 * <p>{@link #reload()} 通过 {@link ResourceBundle#clearCache()} 清空缓存后重新解析，
 * 实现运行期热加载。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public class ResourceBundleMessageSource extends AbstractMessageSource {

    private static final ResourceBundle.Control CONTROL = new ResourceBundle.Control() {
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            if (locale.equals(Locale.ROOT)) {
                return List.of(Locale.ROOT);
            }
            Locale language = locale.getCountry().isEmpty() ? locale : Locale.of(locale.getLanguage());
            return language.equals(locale)
                    ? List.of(locale, Locale.ROOT)
                    : List.of(locale, language, Locale.ROOT);
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            // 禁止 JDK 默认回退到系统默认区域（如 zh 环境使 en 请求误中 zh_CN 包）
            return null;
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            if (!format.equals("java.properties")) {
                return super.newBundle(baseName, locale, format, loader, reload);
            }
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream == null) {
                    return null;
                }
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
        }
    };

    private final String baseName;

    /**
     * 构造消息源。
     *
     * @param baseName 资源包基名（点分隔，如 {@code i18n.messages}），不可为 {@code null}
     */
    public ResourceBundleMessageSource(String baseName) {
        this.baseName = baseName;
    }

    @Override
    protected @Nullable String loadRaw(String code, Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, CONTROL);
            return bundle.containsKey(code) ? bundle.getString(code) : null;
        } catch (java.util.MissingResourceException ex) {
            return null;
        }
    }

    @Override
    public void reload() {
        ResourceBundle.clearCache(Thread.currentThread().getContextClassLoader());
        super.reload();
    }
}
