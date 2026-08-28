package cn.jowen.framework.extras.message.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SimpleTemplateEngine} 渲染验证：{@code ${key}} 占位符替换、缺省值、特殊字符安全。
 */
class SimpleTemplateEngineTest {

    private final SimpleTemplateEngine engine = new SimpleTemplateEngine();

    @Test
    void renderSubstitutesTitleAndContent() {
        Template template = new Template("tpl",
                "你好 ${name}", "验证码 ${code}，请勿泄露");
        var rendered = engine.render(template, Map.of("name", "张三", "code", "1234"));

        assertThat(rendered.title()).isEqualTo("你好 张三");
        assertThat(rendered.content()).isEqualTo("验证码 1234，请勿泄露");
    }

    @Test
    void renderReplacesUnknownKeyWithEmpty() {
        Template template = new Template("tpl", "${missing}", "a${x}b");
        var rendered = engine.render(template, Map.of());

        assertThat(rendered.title()).isEmpty();
        assertThat(rendered.content()).isEqualTo("ab");
    }

    @Test
    void renderReplacesNullValueWithEmpty() {
        Template template = new Template("tpl", "${k}", "${k}");
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("k", null);

        var rendered = engine.render(template, variables);

        assertThat(rendered.title()).isEmpty();
    }

    @Test
    void renderUsesToStringForNonStringValues() {
        Template template = new Template("tpl", "${count}", "${flag}");
        var rendered = engine.render(template, Map.of("count", 42, "flag", true));

        assertThat(rendered.title()).isEqualTo("42");
        assertThat(rendered.content()).isEqualTo("true");
    }

    @Test
    void renderLeavesTextWithoutPlaceholderUnchanged() {
        Template template = new Template("tpl", "纯文本标题", "纯文本内容");
        var rendered = engine.render(template, Map.of("name", "张三"));

        assertThat(rendered.title()).isEqualTo("纯文本标题");
        assertThat(rendered.content()).isEqualTo("纯文本内容");
    }

    @Test
    void renderHandlesSpecialRegexCharactersInValueSafely() {
        // ${} 的值若含 $ \ 等正则元字符，不应破坏 appendReplacement
        Template template = new Template("tpl", "${v}", "${v}");
        var rendered = engine.render(template, Map.of("v", "a$b\\c$1"));

        assertThat(rendered.title()).isEqualTo("a$b\\c$1");
        assertThat(rendered.content()).isEqualTo("a$b\\c$1");
    }

    @Test
    void renderReturnsEmptyForBlankOrNullTemplates() {
        Template template = new Template("tpl", "", null);
        var rendered = engine.render(template, Map.of("k", "v"));

        assertThat(rendered.title()).isEmpty();
        assertThat(rendered.content()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"${a}${b}", "${x}${y}${z}"})
    void renderSupportsMultiplePlaceholdersInOneTemplate(String title) {
        Template template = new Template("tpl", title, "c");
        var rendered = engine.render(template, Map.of("a", "1", "b", "2", "x", "X", "y", "Y", "z", "Z"));

        assertThat(rendered.title()).isEqualTo(title.replace("${a}", "1").replace("${b}", "2")
                .replace("${x}", "X").replace("${y}", "Y").replace("${z}", "Z"));
    }
}
