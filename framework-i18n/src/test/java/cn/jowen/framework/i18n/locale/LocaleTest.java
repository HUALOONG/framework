package cn.jowen.framework.i18n.locale;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocaleTest {

    @Test
    void parsesAcceptLanguageByQuality() {
        List<Locale> locales = LocaleUtils.parseAcceptLanguage("zh-CN, zh;q=0.9, en;q=0.8");
        assertThat(locales).containsExactly(new Locale("zh", "CN"), new Locale("zh"), Locale.ENGLISH);
    }

    @Test
    void parseAcceptLanguageIgnoresZeroQualityAndWildcard() {
        List<Locale> locales = LocaleUtils.parseAcceptLanguage("en;q=0, *, fr");
        assertThat(locales).containsExactly(Locale.FRENCH);
    }

    @Test
    void parsesBcp47Tag() {
        assertThat(LocaleUtils.parseTag("zh-Hans-CN")).isEqualTo(new Locale("zh", "CN"));
        assertThat(LocaleUtils.parseTag("en")).isEqualTo(Locale.ENGLISH);
        assertThat(LocaleUtils.parseTag("  ")).isNull();
    }

    @Test
    void normalizesTag() {
        assertThat(LocaleUtils.normalize("zh-CN")).isEqualTo("zh_CN");
        assertThat(LocaleUtils.normalize("EN")).isEqualTo("en");
    }

    @Test
    void matchesBestCandidate() {
        List<Locale> supported = List.of(Locale.ENGLISH, new Locale("zh", "CN"));
        assertThat(LocaleUtils.match(new Locale("zh", "CN"), supported)).isEqualTo(new Locale("zh", "CN"));
        assertThat(LocaleUtils.match(new Locale("zh"), supported)).isEqualTo(new Locale("zh", "CN"));
        assertThat(LocaleUtils.match(new Locale("fr"), supported)).isEqualTo(Locale.ENGLISH);
        assertThat(LocaleUtils.match(null, List.of())).isNull();
    }

    @Test
    void compositeResolvesFirstNonNull() {
        CompositeLocaleResolver composite = new CompositeLocaleResolver(List.of(
                new FixedLocaleResolver(Locale.ENGLISH),
                new FixedLocaleResolver(new Locale("zh", "CN"))));
        assertThat(composite.resolve(null)).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void fixedResolverIgnoresContext() {
        FixedLocaleResolver resolver = new FixedLocaleResolver(Locale.FRENCH);
        assertThat(resolver.resolve("anything")).isEqualTo(Locale.FRENCH);
    }
}
