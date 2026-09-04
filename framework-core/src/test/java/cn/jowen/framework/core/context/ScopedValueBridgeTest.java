package cn.jowen.framework.core.context;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link ScopedValueBridge} 反射桥接测试：JDK 22+ 验证绑定生效，
 * JDK 21（预览未启用）验证降级路径（null/false，不抛异常）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ScopedValueBridgeTest {

    @Test
    void available_consistentWithInstanceCreation() {
        if (ScopedValueBridge.available()) {
            // JDK 22+：正式 API 可用，能创建实例
            assertThat(ScopedValueBridge.newInstance()).isNotNull();
        } else {
            // JDK 21 预览未启用：实例化被 JVM 拦截，降级为不可用
            assertThat(ScopedValueBridge.newInstance()).isNull();
        }
    }

    @Test
    void unavailable_returnsNullAndFalseWithoutThrowing() {
        Object scopedValue = ScopedValueBridge.newInstance();

        if (scopedValue == null) {
            // 降级路径：所有方法静默返回兜底值
            assertThat(ScopedValueBridge.isBound(scopedValue)).isFalse();
            assertThat(ScopedValueBridge.get(scopedValue)).isNull();
        } else {
            // 可用路径：未绑定时 isBound=false（get 未绑定时 JDK 会抛 NoSuchElementException，不在此断言）
            assertThat(ScopedValueBridge.isBound(scopedValue)).isFalse();
        }
    }

    @Test
    void run_executesTaskOnlyWhenAvailable() {
        Object scopedValue = ScopedValueBridge.newInstance();
        AtomicBoolean executed = new AtomicBoolean(false);

        if (scopedValue != null) {
            ScopedValueBridge.run(scopedValue, "value", () -> {
                executed.set(true);
                assertThat(ScopedValueBridge.get(scopedValue)).isEqualTo("value");
            });
            assertThat(executed).isTrue();
            // 作用域结束后解绑
            assertThat(ScopedValueBridge.isBound(scopedValue)).isFalse();
        } else {
            // 降级路径：任务不执行，也不抛异常
            assertThatCode(() -> ScopedValueBridge.run(new Object(), "value",
                    () -> executed.set(true))).doesNotThrowAnyException();
            assertThat(executed).isFalse();
        }
    }
}
