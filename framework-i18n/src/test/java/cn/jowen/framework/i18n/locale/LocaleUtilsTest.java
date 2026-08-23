package cn.jowen.framework.i18n.locale;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LocaleUtils} 测试。
 */
class LocaleUtilsTest {

    @Test
    void parseAcceptLanguage_nullReturnsEmptyList() {
        assertThat(LocaleUtils.parseAcceptLanguage(null)).isEmpty();
    }

    @Test
    void parseAcceptLanguage_emptyStringReturnsEmptyList() {
        assertThat(LocaleUtils.parseAcceptLanguage("")).isEmpty();
    }

    @Test
    void parseAcceptLanguage_singleLanguage() {
        List<Locale> result = LocaleUtils.parseAcceptLanguage("zh-CN");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLanguage()).isEqualTo("zh");
        assertThat(result.get(0).getCountry()).isEqualTo("CN");
    }

    @Test
    void parseAcceptLanguage_multipleLanguages_sortedByQuality() {
        List<Locale> result = LocaleUtils.parseAcceptLanguage("en;q=0.8, zh-CN;q=1.0");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLanguage()).isEqualTo("zh");
        assertThat(result.get(1).getLanguage()).isEqualTo("en");
    }

    @Test
    void parseAcceptLanguage_qualityZero_excluded() {
        List<Locale> result = LocaleUtils.parseAcceptLanguage("en;q=0, zh-CN");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLanguage()).isEqualTo("zh");
    }

    @Test
    void parseAcceptLanguage_wildcard_excluded() {
        List<Locale> result = LocaleUtils.parseAcceptLanguage("*;q=0.5, zh-CN");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLanguage()).isEqualTo("zh");
    }

    @Test
    void parseTag_zhCN() {
        Locale result = LocaleUtils.parseTag("zh-CN");
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("zh");
        assertThat(result.getCountry()).isEqualTo("CN");
    }

    @Test
    void parseTag_languageOnly() {
        Locale result = LocaleUtils.parseTag("en");
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getCountry()).isEmpty();
    }

    @Test
    void parseTag_nullReturnsNull() {
        assertThat(LocaleUtils.parseTag(null)).isNull();
    }

    @Test
    void parseTag_emptyStringReturnsNull() {
        assertThat(LocaleUtils.parseTag("")).isNull();
    }

    @Test
    void parseTag_zhHansCN_scriptIgnored() {
        Locale result = LocaleUtils.parseTag("zh-Hans-CN");
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("zh");
        assertThat(result.getCountry()).isEqualTo("CN");
    }

    @Test
    void normalize_zhCN_toZh_CN() {
        assertThat(LocaleUtils.normalize("zh-CN")).isEqualTo("zh_CN");
    }

    @Test
    void normalize_uppercase_normalized() {
        assertThat(LocaleUtils.normalize("EN")).isEqualTo("en");
    }

    @Test
    void normalize_null_returnsEmptyString() {
        assertThat(LocaleUtils.normalize(null)).isEqualTo("");
    }

    @Test
    void match_exactMatch_returnsRequested() {
        List<Locale> supported = Arrays.asList(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH);
        Locale result = LocaleUtils.match(Locale.SIMPLIFIED_CHINESE, supported);
        assertThat(result).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    void match_languageOnlyMatch_returnsSupported() {
        List<Locale> supported = Arrays.asList(new Locale("en", "US"), Locale.ENGLISH);
        Locale result = LocaleUtils.match(Locale.ENGLISH, supported);
        assertThat(result).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void match_noMatch_returnsFirstSupported() {
        List<Locale> supported = Arrays.asList(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE);
        Locale result = LocaleUtils.match(Locale.JAPANESE, supported);
        assertThat(result).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void match_emptySupported_returnsNull() {
        assertThat(LocaleUtils.match(Locale.ENGLISH, List.of())).isNull();
    }

    @Test
    void match_nullRequested_usesDefault() {
        List<Locale> supported = Arrays.asList(Locale.getDefault(), Locale.ENGLISH);
        Locale result = LocaleUtils.match(null, supported);
        assertThat(result).isNotNull();
    }
}
