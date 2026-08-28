package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.cache.serializer.JacksonSerializer;
import cn.jowen.framework.cache.serializer.KryoSerializer;
import cn.jowen.framework.core.desensitize.DesensitizeField;
import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry;
import cn.jowen.framework.plugin.api.Plugin;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 各模块 {@link org.springframework.aot.hint.RuntimeHintsRegistrar} 测试。
 */
class RuntimeHintsTest {

    @Test
    void cacheRuntimeHints_registersSerializerReflection() {
        RuntimeHints hints = new RuntimeHints();
        new CacheRuntimeHints().registerHints(hints, null);
        assertThat(hints.reflection().getTypeHint(JacksonSerializer.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(KryoSerializer.class)).isNotNull();
    }

    @Test
    void i18nRuntimeHints_registersResourcePatterns() {
        RuntimeHints hints = mock(RuntimeHints.class);
        ResourceHints resources = mock(ResourceHints.class);
        when(hints.resources()).thenReturn(resources);
        new I18nRuntimeHints().registerHints(hints, null);
        verify(resources).registerPattern("messages*.properties");
        verify(resources).registerPattern("i18n/**/*.properties");
    }

    @Test
    void jdbcRuntimeHints_registersEntityAnnotationReflection() {
        RuntimeHints hints = new RuntimeHints();
        new JdbcRuntimeHints().registerHints(hints, null);
        assertThat(hints.reflection().getTypeHint(Table.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(Id.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(Column.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(GeneratedValue.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(BeanPropertyRowMapper.class)).isNotNull();
    }

    @Test
    void loggerRuntimeHints_registersDesensitizeFieldReflection() {
        RuntimeHints hints = new RuntimeHints();
        new LoggerRuntimeHints().registerHints(hints, null);
        assertThat(hints.reflection().getTypeHint(DesensitizeField.class)).isNotNull();
    }

    @Test
    void mybatisRuntimeHints_registersExtensionReflection() {
        RuntimeHints hints = new RuntimeHints();
        new MybatisRuntimeHints().registerHints(hints, null);
        assertThat(hints.reflection().getTypeHint(ExtensionRegistry.class)).isNotNull();
    }

    @Test
    void spiRuntimeHints_registersSpiReflection() {
        RuntimeHints hints = new RuntimeHints();
        new SpiRuntimeHints().registerHints(hints, null);
        assertThat(hints.reflection().getTypeHint(SPI.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(SPIImplementation.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(Activate.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(Plugin.class)).isNotNull();
    }
}
