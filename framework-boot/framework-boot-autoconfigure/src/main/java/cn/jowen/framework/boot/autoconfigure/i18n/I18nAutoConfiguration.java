package cn.jowen.framework.boot.autoconfigure.i18n;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.i18n.api.LocaleResolver;
import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.i18n.config.FormatterType;
import cn.jowen.framework.i18n.config.ResolverType;
import cn.jowen.framework.i18n.config.SourceType;
import cn.jowen.framework.i18n.format.IcuMessageFormatter;
import cn.jowen.framework.i18n.format.JavaTextMessageFormatter;
import cn.jowen.framework.i18n.format.MessageFormatter;
import cn.jowen.framework.i18n.format.NamedParameterMessageFormatter;
import cn.jowen.framework.i18n.locale.FixedLocaleResolver;
import cn.jowen.framework.i18n.source.AbstractMessageSource;
import cn.jowen.framework.i18n.source.CompositeMessageSource;
import cn.jowen.framework.i18n.source.DatabaseMessageSource;
import cn.jowen.framework.i18n.source.PropertiesMessageSource;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;

/**
 * 国际化装配。向容器提供 {@link MessageSource}（Properties/数据库，可热加载）与
 * {@link LocaleResolver}（参数/固定区域）。
 *
 * <p>策略由 {@code framework.i18n.source|resolver|formatter} 控制：
 * <ul>
 *   <li>source：{@link SourceType#PROPERTIES}（默认）或 {@link SourceType#DATABASE}（需 JdbcTemplate）；</li>
 *   <li>resolver：{@link ResolverType#PARAMETER}（默认，兼容 Map 上下文）
 *       或 {@link ResolverType#FIXED}（单一区域）；</li>
 *   <li>formatter：对 {@link AbstractMessageSource} 子源生效。</li>
 * </ul>
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@ConditionalOnClass(name = "cn.jowen.framework.i18n.source.CompositeMessageSource")
@ConditionalOnProperty(prefix = "framework.i18n", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(BootI18nProperties.class)
public class I18nAutoConfiguration {

    private static void applyFormatter(AbstractMessageSource source, FormatterType type) {
        MessageFormatter formatter = switch (type) {
            case NAMED_PARAMETER -> NamedParameterMessageFormatter.getInstance();
            case ICU -> IcuMessageFormatter.getInstance();
            case JAVA_TEXT -> JavaTextMessageFormatter.getInstance();
        };
        source.setFormatter(formatter);
    }

    /**
     * 消息源（组合包装，便于扩展多源）。
     *
     * @param properties   配置，不可为 {@code null}
     * @param jdbcTemplate JdbcTemplate（DATABASE 源时注入，可为 {@code null}）
     * @param customizers  消息源定制器（可选）
     * @return 消息源，不可为 {@code null}
     */
    @Bean("frameworkMessageSource")
    @ConditionalOnMissingBean(name = "frameworkMessageSource")
    public MessageSource messageSource(BootI18nProperties properties,
                                       @Autowired(required = false) JdbcTemplate jdbcTemplate,
                                       @Autowired(required = false) List<MessageSourceCustomizer> customizers) {
        CompositeMessageSource composite = new CompositeMessageSource();
        if (properties.getSource() == SourceType.DATABASE && jdbcTemplate != null) {
            DatabaseMessageSource dbSource = new DatabaseMessageSource(jdbcTemplate);
            applyFormatter(dbSource, properties.getFormatter());
            composite.add(dbSource);
        } else {
            composite.add(new PropertiesMessageSource(properties.getBasename()));
        }
        if (customizers != null) {
            for (MessageSourceCustomizer customizer : customizers) {
                customizer.customize(composite);
            }
        }
        return composite;
    }

    /**
     * 区域解析器：按 {@code framework.i18n.resolver} 选择策略。
     *
     * @param properties 配置，不可为 {@code null}
     * @return 区域解析器，不可为 {@code null}
     */
    @Bean("frameworkLocaleResolver")
    @ConditionalOnMissingBean(name = "frameworkLocaleResolver")
    public LocaleResolver localeResolver(BootI18nProperties properties) {
        Locale fallback = properties.getDefaultLocale().isEmpty()
                ? Locale.getDefault()
                : Locale.forLanguageTag(properties.getDefaultLocale().replace('_', '-'));
        if (properties.getResolver() == ResolverType.FIXED) {
            return new FixedLocaleResolver(fallback);
        }
        return context -> {
            if (context instanceof java.util.Map<?, ?> map) {
                Object lang = map.get("lang");
                if (lang instanceof String s && !s.isEmpty()) {
                    return Locale.forLanguageTag(s.replace('_', '-'));
                }
            }
            return fallback;
        };
    }
}
