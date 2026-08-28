package cn.jowen.framework.i18n.source;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisMessageSource} 键映射逻辑测试（Redis 键策略不依赖真实连接）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class RedisMessageSourceTest {

    @Test
    void mapKey_languageOnlyLocale() {
        assertThat(RedisMessageSource.mapKey("messages", Locale.US)).isEqualTo("i18n:messages:en-US");
    }

    @Test
    void mapKey_fullLocaleUsesLanguageTag() {
        assertThat(RedisMessageSource.mapKey("messages", Locale.forLanguageTag("zh-CN")))
                .isEqualTo("i18n:messages:zh-CN");
    }

    @Test
    void mapKey_rootLocaleUsesRootMarker() {
        assertThat(RedisMessageSource.mapKey("messages", Locale.ROOT)).isEqualTo("i18n:messages:root");
    }
}