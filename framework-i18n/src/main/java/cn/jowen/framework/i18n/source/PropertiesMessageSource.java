package cn.jowen.framework.i18n.source;

import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import cn.jowen.framework.i18n.reload.ResourceReloader.CountableMessageSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于资源包的 {@link ReloadableMessageSource}。
 *
 * <p>资源来源由 basename 决定：
 * <ul>
 *   <li>{@code file:/绝对路径/目录/messages}：文件系统（推荐生产环境，配合
 *       {@code FileWatchResourceWatcher} 实现热加载）；</li>
 *   <li>其余（如 {@code i18n/messages}）：classpath 资源。</li>
 * </ul>
 *
 * <p>命名约定：{@code basename[_语言[_国家]].properties}。解析时按精确区域 → 语言 → 默认回退。
 *
 * <p>重载一致性（原子替换）：{@link #reload()} 基于已加载区域重建全部资源包后整体替换缓存引用；
 * 重载期间新请求继续读取旧快照，重载完成后一次性原子切换，避免读到半更新状态。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class PropertiesMessageSource implements ReloadableMessageSource, CountableMessageSource {

    private final String basename;
    /**
     * locale 键 → 资源包；volatile 引用实现原子替换。
     */
    private volatile Map<String, Properties> bundles = new ConcurrentHashMap<>();

    public PropertiesMessageSource(String basename) {
        this.basename = basename;
        reload();
    }

    private static String localeKey(Locale locale) {
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    private static Locale localeFor(String key) {
        if (key.isEmpty()) {
            return Locale.ROOT;
        }
        int idx = key.indexOf('_');
        return idx < 0 ? Locale.of(key) : Locale.of(key.substring(0, idx), key.substring(idx + 1));
    }

    @Override
    public @Nullable String getMessage(String code, Locale locale, @Nullable Object @Nullable [] args) {
        String raw = resolveRaw(code, locale);
        if (raw == null) {
            return null;
        }
        return (args == null || args.length == 0) ? raw : new MessageFormat(raw, locale).format(args);
    }

    @Override
    public boolean contains(String code, Locale locale) {
        return resolveRaw(code, locale) != null;
    }

    @Override
    public int messageCount() {
        int count = 0;
        for (Properties props : bundles.values()) {
            count += props.size();
        }
        return count;
    }

    @Override
    public void reload() {
        Map<String, Properties> current = bundles;
        Map<String, Properties> next = new ConcurrentHashMap<>();
        for (Map.Entry<String, Properties> entry : current.entrySet()) {
            Locale locale = localeFor(entry.getKey());
            Properties fresh = loadBundle(basename, locale);
            // 资源临时不可读（如文件被短暂删除）时保留旧值，避免文案丢失
            if (fresh.isEmpty() && !entry.getValue().isEmpty()) {
                fresh = entry.getValue();
            }
            next.put(entry.getKey(), fresh);
        }
        bundles = next;
    }

    private @Nullable String resolveRaw(String code, Locale locale) {
        Map<String, Properties> current = bundles;
        String key = localeKey(locale);
        Properties props = current.computeIfAbsent(key, k -> loadBundle(basename, locale));
        String value = props.getProperty(code);
        if (value != null) {
            return value;
        }
        // 回退：语言级别
        if (!locale.getCountry().isEmpty()) {
            String langKey = locale.getLanguage();
            Properties langProps = current.computeIfAbsent(langKey, k -> loadBundle(basename, Locale.of(langKey)));
            value = langProps.getProperty(code);
            if (value != null) {
                return value;
            }
        }
        // 回退：默认（无后缀）
        Properties defaultProps = current.computeIfAbsent("", k -> loadBundle(basename, null));
        return defaultProps.getProperty(code);
    }

    private Properties loadBundle(String base, @Nullable Locale locale) {
        Properties props = new Properties();
        String suffix = locale == null || locale == Locale.ROOT ? "" : "_" + locale.getLanguage()
                + (locale.getCountry().isEmpty() ? "" : "_" + locale.getCountry());
        if (base.startsWith("file:")) {
            loadFromFile(base.substring("file:".length()) + suffix + ".properties", props);
        } else {
            loadFromClasspath(base.replace('.', '/') + suffix + ".properties", props);
        }
        return props;
    }

    private void loadFromFile(String path, Properties props) {
        Path file = Path.of(path);
        if (!Files.exists(file)) {
            return; // 资源不存在属正常回退场景，返回空
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取资源包失败: " + file, ex);
        }
    }

    private void loadFromClasspath(String path, Properties props) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // 资源不存在属正常回退场景，忽略
        }
    }
}
