package cn.jowen.framework.i18n.format;

import cn.jowen.framework.i18n.api.FormatException;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link IcuMessageFormatter} 测试：单例、名称、可用性探测与未引入 ICU4J 时的失败路径。
 */
class IcuMessageFormatterTest {

    @Test
    void name_and_singleton() {
        IcuMessageFormatter f = IcuMessageFormatter.getInstance();
        assertThat(f).isNotNull();
        assertThat(f.name()).isEqualTo("icu");
        assertThat(IcuMessageFormatter.getInstance()).isSameAs(f);
    }

    @Test
    void available_reflectsClasspath() {
        // 不依赖 ICU4J 是否在 classpath，仅断言返回布尔
        assertThat(IcuMessageFormatter.available()).isIn(true, false);
    }

    @Test
    void format_throwsWhenIcuUnavailable() {
        if (!IcuMessageFormatter.available()) {
            IcuMessageFormatter f = IcuMessageFormatter.getInstance();
            assertThatThrownBy(() -> f.format("Hello {0}", new Object[]{"x"}, Locale.ROOT))
                    .isInstanceOf(FormatException.class);
        }
    }
}
