package cn.jowen.framework.extras.notification.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SimpleTemplateEngine} 测试：占位符替换 / 缺失保留 / 多值。
 */
class SimpleTemplateEngineTest {

    private final SimpleTemplateEngine engine = new SimpleTemplateEngine();

    @Test
    void render_replacesPlaceholders() {
        String result = engine.render("您好 ${name}，订单 ${orderId} 已创建", Map.of("name", "张三", "orderId", "10086"));
        assertThat(result).isEqualTo("您好 张三，订单 10086 已创建");
    }

    @Test
    void render_missingPlaceholder_keepsOriginal() {
        String result = engine.render("欢迎 ${name}", Map.of("other", "x"));
        assertThat(result).isEqualTo("欢迎 ${name}");
    }

    @Test
    void render_nullValue_keepsOriginal() {
        // singletonMap 允许 null 值（Map.of 不允许）
        String result = engine.render("值=${k}", java.util.Collections.singletonMap("k", null));
        assertThat(result).isEqualTo("值=${k}");
    }

    @Test
    void render_specialChars_escaped() {
        String result = engine.render("路径 ${p}", Map.of("p", "a$b\\c"));
        assertThat(result).isEqualTo("路径 a$b\\c");
    }

    @Test
    void render_emptyTemplate_returnsEmpty() {
        assertThat(engine.render("", Map.of())).isEmpty();
    }

    @Test
    void render_nullArgs_throw() {
        assertThatThrownBy(() -> engine.render(null, Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> engine.render("x", null)).isInstanceOf(IllegalArgumentException.class);
    }
}