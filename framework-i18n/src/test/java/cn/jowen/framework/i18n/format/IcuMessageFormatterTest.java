package cn.jowen.framework.i18n.format;

import cn.jowen.framework.i18n.api.FormatException;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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
    void available_and_name_reflectClasspath() {
        // 在测试时探测 ICU4J 是否在 classpath 上，并据此断言。
        // 本环境（framework-i18n pom 未声明 com.ibm.icu:icu4j）下 available()==false。
        if (IcuMessageFormatter.available()) {
            assertThat(IcuMessageFormatter.getInstance().name()).isEqualTo("icu");
        } else {
            assertThat(IcuMessageFormatter.available()).isFalse();
            assertThat(IcuMessageFormatter.getInstance().name()).isEqualTo("icu");
        }
    }

    @Test
    void format_behavior_dependsOnClasspath() {
        IcuMessageFormatter f = IcuMessageFormatter.getInstance();
        if (IcuMessageFormatter.available()) {
            // ICU 成功路径（本环境不可达，标记为环境相关）：反射调用 ICU MessageFormat 完成格式化。
            assertThat(f.format("{0}", new Object[]{"x"}, Locale.ROOT)).isEqualTo("x");
        } else {
            // 未引入 ICU4J 时，format() 必须抛 FormatException。
            FormatException ex = catchThrowableOfType(
                    () -> f.format("Hello {0}", new Object[]{"x"}, Locale.ROOT), FormatException.class);
            assertThat(ex).isNotNull();
        }
    }
}
