package cn.jowen.framework.i18n.source;

import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.i18n.api.ResourceLoadException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DatabaseMessageSource} 测试。
 */
class DatabaseMessageSourceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void load_exactLocaleHit() {
        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("zh_CN", "greeting", "你好"),
                row("", "greeting", "Hello")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate);

        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("你好");
        assertThat(source.contains("greeting", Locale.CHINA)).isTrue();
    }

    @Test
    void load_fallsBackToLanguage() {
        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("zh", "greeting", "你好")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate);

        // zh_CN 无精确，回退到语言 zh
        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("你好");
    }

    @Test
    void load_fallsBackToRoot() {
        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("", "greeting", "Hello")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate);

        assertThat(source.getMessage("greeting", Locale.US, null)).isEqualTo("Hello");
        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("Hello");
    }

    @Test
    void load_missing_returnsNull() {
        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("", "greeting", "Hello")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate);

        assertThat(source.getMessage("nope", Locale.US, null)).isNull();
        assertThat(source.contains("nope", Locale.US)).isFalse();
    }

    @Test
    void reload_rebuildsCache() {
        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("", "greeting", "Hello")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate);
        assertThat(source.getMessage("greeting", Locale.US, null)).isEqualTo("Hello");

        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("", "greeting", "Hi")));
        source.reload();
        assertThat(source.getMessage("greeting", Locale.US, null)).isEqualTo("Hi");
    }

    @Test
    void localeCount_countsDistinctLocales() {
        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("zh_CN", "a", "1"),
                row("zh_CN", "b", "2"),
                row("en", "a", "3")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate);

        assertThat(source.localeCount()).isEqualTo(2);
    }

    @Test
    void nullColumnRows_skipped() {
        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("zh_CN", "a", "1"),
                rowWithNull("b", "2"),
                rowWithNullCode("3")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate);

        assertThat(source.localeCount()).isEqualTo(1);
        assertThat(source.getMessage("a", Locale.CHINA, null)).isEqualTo("1");
    }

    @Test
    void format_appliesArguments() {
        when(jdbcTemplate.queryForMaps(anyString())).thenReturn(List.of(
                row("", "welcome", "Welcome, {0}")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate);

        assertThat(source.getMessage("welcome", Locale.US, new Object[]{"Tom"})).isEqualTo("Welcome, Tom");
    }

    @Test
    void queryFailure_throwsResourceLoadException() {
        when(jdbcTemplate.queryForMaps(anyString())).thenThrow(new IllegalStateException("db down"));
        assertThatThrownBy(() -> new DatabaseMessageSource(jdbcTemplate))
                .isInstanceOf(ResourceLoadException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void customTableAndColumns_used() {
        when(jdbcTemplate.queryForMaps("SELECT lang, key, value FROM t_i18n"))
                .thenReturn(List.of(rowWithColumns("lang", "key", "value", "", "greeting", "Hi")));
        DatabaseMessageSource source = new DatabaseMessageSource(jdbcTemplate, "t_i18n", "lang", "key", "value");

        assertThat(source.getMessage("greeting", Locale.US, null)).isEqualTo("Hi");
    }

    private static Map<String, Object> row(String locale, String code, String message) {
        return Map.of("locale", locale, "code", code, "message", message);
    }

    private static Map<String, Object> rowWithColumns(String localeColumn, String codeColumn, String messageColumn,
                                                      String locale, String code, String message) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put(localeColumn, locale);
        map.put(codeColumn, code);
        map.put(messageColumn, message);
        return map;
    }

    private static Map<String, Object> rowWithNull(String code, String message) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("locale", null);
        map.put("code", code);
        map.put("message", message);
        return map;
    }

    private static Map<String, Object> rowWithNullCode(String message) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("locale", "en");
        map.put("code", null);
        map.put("message", message);
        return map;
    }
}
