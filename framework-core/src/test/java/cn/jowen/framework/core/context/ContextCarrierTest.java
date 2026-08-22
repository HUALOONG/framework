package cn.jowen.framework.core.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link ContextCarrier} 双模式上下文载体测试。
 */
class ContextCarrierTest {

    private static final ContextKey<String> TENANT = ContextKey.named("tenantId", String.class);
    private static final ContextKey<Long> USER_ID = ContextKey.named("userId", Long.class);

    @AfterEach
    void reset() {
        ContextCarrier.configure(ContextCarrier.Mode.SCOPED_VALUE);
    }

    @ParameterizedTest
    @EnumSource(ContextCarrier.Mode.class)
    void runWith_bindsValueInScope(ContextCarrier.Mode mode) {
        ContextCarrier.configure(mode);
        ContextCarrier.runWith(TENANT, "t-1001", () -> {
            assertThat(ContextCarrier.get(TENANT)).isEqualTo("t-1001");
        });
        // 作用域外不可见
        assertThat(ContextCarrier.get(TENANT)).isNull();
    }

    @ParameterizedTest
    @EnumSource(ContextCarrier.Mode.class)
    void nested_scopeOverridesAndRestores(ContextCarrier.Mode mode) {
        ContextCarrier.configure(mode);
        ContextCarrier.runWith(TENANT, "outer", () -> {
            ContextCarrier.runWith(TENANT, "inner", () -> {
                assertThat(ContextCarrier.get(TENANT)).isEqualTo("inner");
            });
            assertThat(ContextCarrier.get(TENANT)).isEqualTo("outer");
        });
        assertThat(ContextCarrier.get(TENANT)).isNull();
    }

    @Test
    void scopedValue_setOutsideScope_throws() {
        org.junit.jupiter.api.Assumptions.assumeTrue(ScopedValueBridge.available(),
                "ScopedValue 仅 JDK 22+ 或启用预览时可用");
        assertThatThrownBy(() -> ContextCarrier.set(TENANT, "x"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void threadLocal_setKeepsValueOnThread() {
        ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
        ContextCarrier.set(TENANT, "t-1001");
        try {
            assertThat(ContextCarrier.get(TENANT)).isEqualTo("t-1001");
        } finally {
            ContextCarrier.set(TENANT, null); // 清理，避免污染其他用例
        }
    }

    @ParameterizedTest
    @EnumSource(ContextCarrier.Mode.class)
    void snapshot_isImmutableAndReplayable(ContextCarrier.Mode mode) {
        ContextCarrier.configure(mode);
        ContextCarrier.runWith(TENANT, "t-1001", () -> {
            ContextCarrier.runWith(USER_ID, 42L, () -> {
                ContextSnapshot snapshot = ContextCarrier.snapshot();
                assertThat(snapshot.values()).containsEntry(TENANT, "t-1001");

                // 修改当前上下文不影响已捕获快照
                ContextCarrier.set(TENANT, "changed");
                snapshot.replay(() -> {
                    assertThat(ContextCarrier.get(TENANT)).isEqualTo("t-1001");
                });
                assertThat(ContextCarrier.get(TENANT)).isEqualTo("changed");
            });
        });
    }

    @Test
    void scopedValue_notInheritedByVirtualThread() throws Exception {
        // ScopedValue 默认模式：虚拟线程不自动继承上下文
        ContextCarrier.runWith(TENANT, "t-1001", () -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var future = executor.submit(() -> ContextCarrier.get(TENANT));
                assertThat(future.get()).isNull();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void capture_returnsEmptyWhenNothingBound() {
        assertThat(ContextSnapshot.capture().isEmpty()).isTrue();
    }
}
