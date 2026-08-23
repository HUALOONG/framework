package cn.jowen.framework.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link ContextCarrier} 测试（强制使用 ThreadLocal 模式以确保确定性）。
 */
class ContextCarrierTest {

    private static final ContextKey<String> TENANT = ContextKey.named("tenant", String.class);
    private static final ContextKey<Integer> LEVEL = ContextKey.named("level", Integer.class);

    private final ContextCarrier.Mode originalMode = ContextCarrier.mode();

    @BeforeEach
    void setUp() {
        ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
    }

    @AfterEach
    void tearDown() {
        ContextCarrier.configure(originalMode);
        try {
            Field tlField = ContextCarrier.class.getDeclaredField("THREAD_LOCAL_VALUES");
            tlField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ThreadLocal<Map<ContextKey<?>, Object>> tl = (ThreadLocal<Map<ContextKey<?>, Object>>) tlField.get(null);
            tl.remove();
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Test
    void set_and_get() {
        ContextCarrier.set(TENANT, "t1");
        assertThat(ContextCarrier.get(TENANT)).isEqualTo("t1");
    }

    @Test
    void get_beforeSet_returnsNull() {
        assertThat(ContextCarrier.get(TENANT)).isNull();
    }

    @Test
    void set_null_removesKey() {
        ContextCarrier.set(TENANT, "t1");
        ContextCarrier.set(TENANT, (String) null);
        assertThat(ContextCarrier.get(TENANT)).isNull();
    }

    @Test
    void runWith_bindsValueInsideTask() {
        ContextCarrier.set(TENANT, "outside");
        ContextCarrier.runWith(TENANT, "inside", () ->
                assertThat(ContextCarrier.get(TENANT)).isEqualTo("inside"));
        assertThat(ContextCarrier.get(TENANT)).isEqualTo("outside");
    }

    @Test
    void runWith_removesKeyWithValueNull() {
        ContextCarrier.set(TENANT, "existing");
        ContextCarrier.runWith(TENANT, (String) null, () ->
                assertThat(ContextCarrier.get(TENANT)).isNull());
        assertThat(ContextCarrier.get(TENANT)).isEqualTo("existing");
    }

    @Test
    void snapshot_and_replay() {
        ContextCarrier.set(TENANT, "snap");
        ContextCarrier.set(LEVEL, 99);
        ContextSnapshot snap = ContextCarrier.snapshot();

        ContextCarrier.set(TENANT, "changed");

        ContextCarrier.replay(snap, () -> {
            assertThat(ContextCarrier.get(TENANT)).isEqualTo("snap");
            assertThat(ContextCarrier.get(LEVEL)).isEqualTo(99);
        });

        assertThat(ContextCarrier.get(TENANT)).isEqualTo("changed");
    }

    @Test
    void snapshot_empty() {
        ContextSnapshot snap = ContextCarrier.snapshot();
        assertThat(snap.isEmpty()).isTrue();
    }

    @Test
    void typeMismatch_returnsNull() {
        ContextCarrier.set(TENANT, "hello");
        assertThat(ContextCarrier.get(LEVEL)).isNull();
    }

    @Test
    void nested_runWith_stacksCorrectly() {
        ContextCarrier.runWith(TENANT, "outer", () -> {
            assertThat(ContextCarrier.get(TENANT)).isEqualTo("outer");
            ContextCarrier.runWith(TENANT, "inner", () ->
                    assertThat(ContextCarrier.get(TENANT)).isEqualTo("inner"));
            assertThat(ContextCarrier.get(TENANT)).isEqualTo("outer");
        });
    }

    @Test
    void runWith_nullKey_throws() {
        assertThatThrownBy(() -> ContextCarrier.runWith(null, "v", () -> { }))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void runWith_nullTask_throws() {
        assertThatThrownBy(() -> ContextCarrier.runWith(TENANT, "v", null))
                .isInstanceOf(NullPointerException.class);
    }
}