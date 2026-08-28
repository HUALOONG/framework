package cn.jowen.framework.i18n.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LocaleMatcher} 测试。
 */
class LocaleMatcherTest {

    private static final List<Locale> SUPPORTED = List.of(Locale.CHINA, Locale.US, Locale.JAPAN);

    @Test
    void bestMatch_exactMatch() {
        assertThat(LocaleMatcher.bestMatch(Locale.CHINA, SUPPORTED)).isEqualTo(Locale.CHINA);
    }

    @Test
    void bestMatch_languageMatch() {
        // zh_TW 无精确，回退语言 zh → CHINA
        assertThat(LocaleMatcher.bestMatch(Locale.TAIWAN, SUPPORTED)).isEqualTo(Locale.CHINA);
    }

    @Test
    void bestMatch_noLanguageMatch_returnsFirst() {
        assertThat(LocaleMatcher.bestMatch(Locale.FRANCE, SUPPORTED)).isEqualTo(Locale.CHINA);
    }

    @Test
    void bestMatch_nullRequested_usesDefault() {
        assertThat(LocaleMatcher.bestMatch(null, SUPPORTED))
                .isEqualTo(LocaleMatcher.DEFAULT);
    }

    @Test
    void bestMatch_emptySupported_returnsNull() {
        assertThat(LocaleMatcher.bestMatch(Locale.US, List.of())).isNull();
    }

    @Test
    void exact_bothNull_false() {
        assertThat(LocaleMatcher.exact(null, null)).isFalse();
        assertThat(LocaleMatcher.exact(null, Locale.US)).isFalse();
        assertThat(LocaleMatcher.exact(Locale.US, null)).isFalse();
    }

    @Test
    void exact_sameLanguageAndCountry_true() {
        assertThat(LocaleMatcher.exact(Locale.CHINA, Locale.of("zh", "CN"))).isTrue();
        assertThat(LocaleMatcher.exact(Locale.CHINA, Locale.US)).isFalse();
    }

    @Test
    void language_bothNull_false() {
        assertThat(LocaleMatcher.language(null, null)).isFalse();
        assertThat(LocaleMatcher.language(null, Locale.US)).isFalse();
        assertThat(LocaleMatcher.language(Locale.US, null)).isFalse();
    }

    @Test
    void language_sameLanguage_true() {
        assertThat(LocaleMatcher.language(Locale.CHINA, Locale.TAIWAN)).isTrue();
        assertThat(LocaleMatcher.language(Locale.CHINA, Locale.US)).isFalse();
    }
}
