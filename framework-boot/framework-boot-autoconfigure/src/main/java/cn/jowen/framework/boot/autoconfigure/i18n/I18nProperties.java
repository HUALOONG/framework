package cn.jowen.framework.boot.autoconfigure.i18n;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 国际化配置属性。绑定 {@code framework.i18n.*}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.i18n")
public class I18nProperties {

    /** 是否启用框架国际化装配，缺省开启。 */
    private boolean enabled = true;

    /** 消息源类型，缺省 {@link SourceType#PROPERTIES}。 */
    private SourceType source = SourceType.PROPERTIES;

    /** 资源包基名（classpath 下，点分隔），如 {@code i18n.messages}，缺省 {@code messages}。 */
    private String basename = "messages";

    /** 区域解析策略，缺省 {@link ResolverType#PARAMETER}。 */
    private ResolverType resolver = ResolverType.PARAMETER;

    /** 消息格式化策略，缺省 {@link FormatterType#JAVA_TEXT}。 */
    private FormatterType formatter = FormatterType.JAVA_TEXT;

    /** 默认区域，如 {@code zh_CN}，缺省使用 JVM 默认区域。 */
    private String defaultLocale = "";

    /** 热加载轮询间隔（秒），0 表示不轮询，缺省 0。 */
    private long reloadIntervalSeconds = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SourceType getSource() {
        return source;
    }

    public void setSource(SourceType source) {
        this.source = source;
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    public ResolverType getResolver() {
        return resolver;
    }

    public void setResolver(ResolverType resolver) {
        this.resolver = resolver;
    }

    public FormatterType getFormatter() {
        return formatter;
    }

    public void setFormatter(FormatterType formatter) {
        this.formatter = formatter;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public long getReloadIntervalSeconds() {
        return reloadIntervalSeconds;
    }

    public void setReloadIntervalSeconds(long reloadIntervalSeconds) {
        this.reloadIntervalSeconds = reloadIntervalSeconds;
    }
}
