package cn.jowen.framework.core.util;

import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ReflectionUtils} 覆盖率补齐测试：针对 JaCoCo 报告的独立口径缺口行。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>{@code getAllFields} 跳过 synthetic 字段（内部类 this$0）；</li>
 *   <li>{@code getMethod} 对私有方法执行 setAccessible；</li>
 *   <li>{@code findMethod} 候选为空时抛 SystemException；</li>
 *   <li>{@code accessible} 对非公有方法执行 setAccessible（经 invokeMethod 间接触发）；</li>
 *   <li>{@code invokeMethod(target, method, args)} 方法不可访问时包装 IllegalAccessException。</li>
 * </ul>
 *
 * @author Yan
 * @since 0.0.1
 * @version 0.0.1
 */
class ReflectionUtilsGapCoverageTest {

    /**
     * 外层类：非静态内部类 {@link SynInner} 编译器为其生成 synthetic {@code this$0} 字段（指向外层实例）。
     */
    static class SynHolder {
        class SynInner {
            private String value = "syn";
        }
    }

    /**
     * 含私有方法的持有类。
     */
    static class PrivateMethodHolder {
        private String secret() {
            return "s";
        }
    }

    /**
     * 含私有方法的持有类（用于不可访问方法调用测试）。
     */
    static class InaccessibleMethodHolder {
        private String hidden() {
            return "h";
        }
    }

    @Test
    void getAllFields_skipsSyntheticFields() {
        // SynInner 是非静态内部类，编译器为其生成 synthetic 字段 this$0
        List<Field> fields = ReflectionUtils.getAllFields(SynHolder.SynInner.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        // synthetic 字段 this$0 必须被跳过
        assertThat(names).doesNotContain("this$0");
        // 非 synthetic 字段 value 应存在
        assertThat(names).contains("value");
    }

    @Test
    void getMethod_findsPrivateMethod_setsAccessible() {
        Method m = ReflectionUtils.getMethod(PrivateMethodHolder.class, "secret");
        assertThat(m).isNotNull();
        assertThat(m.getName()).isEqualTo("secret");
    }

    @Test
    void findMethod_noCandidates_throws() {
        PrivateMethodHolder h = new PrivateMethodHolder();
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(h, "nonExistent"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("方法不存在");
    }

    @Test
    void invokeMethod_privateMethod_viaFindMethod_coversAccessible() {
        PrivateMethodHolder h = new PrivateMethodHolder();
        // 经 findMethod → accessible() → setAccessible(true) 路径触发
        Object result = ReflectionUtils.invokeMethod(h, "secret");
        assertThat(result).isEqualTo("s");
    }

    @Test
    void invokeMethod_withMethod_inaccessible_throwsSystemException() throws Exception {
        // 获取私有方法但不设 accessible，invoke() 抛 IllegalAccessException → 被包装为 SystemException
        Method m = InaccessibleMethodHolder.class.getDeclaredMethod("hidden");
        InaccessibleMethodHolder h = new InaccessibleMethodHolder();
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(h, m))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("方法调用失败");
    }
}
