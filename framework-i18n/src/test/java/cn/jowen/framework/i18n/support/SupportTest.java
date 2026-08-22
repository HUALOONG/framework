package cn.jowen.framework.i18n.support;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportTest {

    @Test
    void messageCodeJoinsAndNormalizes() {
        assertThat(MessageCodeUtils.join("user", "validate", "nameRequired"))
                .isEqualTo("user.validate.nameRequired");
        assertThat(MessageCodeUtils.withPrefix("user", "validate.nameRequired"))
                .isEqualTo("user.validate.nameRequired");
        assertThat(MessageCodeUtils.withPrefix("user", "user.validate.x")).isEqualTo("user.validate.x");
        assertThat(MessageCodeUtils.normalize("user_validate-x")).isEqualTo("user.validate.x");
        assertThat(MessageCodeUtils.isInvalid("  ")).isTrue();
    }

    @Test
    void placeholderResolverSubstitutes() {
        PlaceholderResolver resolver = PlaceholderResolver.getInstance();
        String result = resolver.resolve("Hi ${name}, you have ${count} items",
                Map.of("name", "Jowen", "count", 3));
        assertThat(result).isEqualTo("Hi Jowen, you have 3 items");
        assertThat(resolver.hasPlaceholders("${x}")).isTrue();
        assertThat(resolver.hasPlaceholders("plain")).isFalse();
    }

    @Test
    void propertiesFileParserLoadsClasspath() {
        var props = PropertiesFileParser.loadFromClasspath("i18n/messages.properties");
        assertThat(props.getProperty("greeting")).isEqualTo("Hello");
    }

    @Test
    void localeMatcherBestMatch() {
        List<Locale> supported = List.of(Locale.ENGLISH, new Locale("zh", "CN"));
        assertThat(LocaleMatcher.bestMatch(new Locale("zh", "CN"), supported)).isEqualTo(new Locale("zh", "CN"));
        assertThat(LocaleMatcher.bestMatch(new Locale("zh"), supported)).isEqualTo(new Locale("zh", "CN"));
        assertThat(LocaleMatcher.bestMatch(new Locale("fr"), supported)).isEqualTo(Locale.ENGLISH);
        assertThat(LocaleMatcher.exact(new Locale("zh", "CN"), new Locale("zh", "CN"))).isTrue();
        assertThat(LocaleMatcher.language(new Locale("zh", "CN"), new Locale("zh", "TW"))).isTrue();
    }
}
