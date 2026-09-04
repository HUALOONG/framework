package cn.jowen.framework.boot.autoconfigure.i18n;

import cn.jowen.framework.i18n.api.LocaleResolver;
import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.boot.autoconfigure.i18n.BootI18nProperties;
import cn.jowen.framework.i18n.config.FormatterType;
import cn.jowen.framework.i18n.config.ResolverType;
import cn.jowen.framework.i18n.config.SourceType;
import cn.jowen.framework.i18n.locale.FixedLocaleResolver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link I18nAutoConfiguration} 测试：直接调用 {@code @Bean} 方法，覆盖各消息源分支、
 * 定制器、指标收集与区域解析策略，无需完整 Spring 上下文。
 */
class I18nAutoConfigurationTest {

    private final I18nAutoConfiguration config = new I18nAutoConfiguration();

    @Test
    void propertiesSource_defaultFallback() {
        BootI18nProperties props = new BootI18nProperties();
        props.setSource(SourceType.PROPERTIES);
        MessageSource source = config.messageSource(props, null, null,
                nullProvider(MeterRegistry.class), nullProvider(RedissonClient.class));
        assertThat(source).isNotNull();
    }

    @Test
    void redisSource_withClient_andEachFormatter() {
        for (FormatterType fmt : FormatterType.values()) {
            BootI18nProperties props = new BootI18nProperties();
            props.setSource(SourceType.REDIS);
            props.setFormatter(fmt);
            RedissonClient client = mock(RedissonClient.class);
            MessageSource source = config.messageSource(props, null, null,
                    nullProvider(MeterRegistry.class), withProvider(client));
            assertThat(source).isNotNull();
        }
    }

    @Test
    void redisSource_withoutClient_fallsBackToProperties() {
        BootI18nProperties props = new BootI18nProperties();
        props.setSource(SourceType.REDIS);
        MessageSource source = config.messageSource(props, null, null,
                nullProvider(MeterRegistry.class), nullProvider(RedissonClient.class));
        assertThat(source).isNotNull();
    }

    @Test
    void customizers_areAppliedToComposite() {
        BootI18nProperties props = new BootI18nProperties();
        props.setSource(SourceType.PROPERTIES);
        MessageSourceCustomizer customizer = mock(MessageSourceCustomizer.class);
        MessageSource source = config.messageSource(props, null, List.of(customizer),
                nullProvider(MeterRegistry.class), nullProvider(RedissonClient.class));
        verify(customizer).customize(source);
    }

    @Test
    void meterRegistry_wrapsInMetricsCollector() {
        BootI18nProperties props = new BootI18nProperties();
        props.setSource(SourceType.PROPERTIES);
        MeterRegistry registry = new SimpleMeterRegistry();
        MessageSource source = config.messageSource(props, null, null,
                withProvider(registry), nullProvider(RedissonClient.class));
        assertThat(source).isNotNull();
    }

    @Test
    void localeResolver_parameterAndFixed() {
        BootI18nProperties parameterProps = new BootI18nProperties();
        parameterProps.setResolver(ResolverType.PARAMETER);
        assertThat(config.localeResolver(parameterProps)).isNotNull();

        BootI18nProperties fixedProps = new BootI18nProperties();
        fixedProps.setResolver(ResolverType.FIXED);
        fixedProps.setDefaultLocale("zh_CN");
        assertThat(config.localeResolver(fixedProps)).isInstanceOf(FixedLocaleResolver.class);
    }

    @Test
    void databaseSource_withUnusableDataSource_surfacesResourceLoadException() {
        BootI18nProperties props = new BootI18nProperties();
        props.setSource(SourceType.DATABASE);
        // DatabaseMessageSource 构造期即 reload()；数据源不可用时包装为 ResourceLoadException 抛出
        DataSource dataSource = mock(DataSource.class);

        assertThatThrownBy(() -> config.messageSource(props, dataSource, null,
                nullProvider(MeterRegistry.class), nullProvider(RedissonClient.class)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("加载数据库消息失败");
    }

    @Test
    void databaseSource_withoutDataSource_fallsBackToProperties() {
        BootI18nProperties props = new BootI18nProperties();
        props.setSource(SourceType.DATABASE);
        // DATABASE 源但无 DataSource：落入 else 分支使用 Properties 兜底
        MessageSource source = config.messageSource(props, null, null,
                nullProvider(MeterRegistry.class), nullProvider(RedissonClient.class));
        assertThat(source).isNotNull();
    }

    @Test
    void localeResolver_parameter_resolvesLangFromMapAndFallback() {
        BootI18nProperties props = new BootI18nProperties();
        props.setResolver(ResolverType.PARAMETER);
        LocaleResolver resolver = config.localeResolver(props);

        // Map 上下文且带 lang
        assertThat(resolver.resolve(Map.of("lang", "en_US")))
                .isEqualTo(Locale.forLanguageTag("en-US"));
        // Map 上下文但无 lang（空串/缺失）-> 回退默认区域
        assertThat(resolver.resolve(Map.of())).isEqualTo(Locale.getDefault());
        assertThat(resolver.resolve(Map.of("lang", ""))).isEqualTo(Locale.getDefault());
        // 非 Map 上下文 -> 回退默认区域
        assertThat(resolver.resolve("not a map")).isEqualTo(Locale.getDefault());
        assertThat(resolver.resolve(null)).isEqualTo(Locale.getDefault());
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> nullProvider(Class<T> type) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> withProvider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
