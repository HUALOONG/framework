package cn.jowen.framework.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link ContextSnapshot} 测试。
 */
class ContextSnapshotTest {

    private static final ContextKey<String> A = ContextKey.named("a", String.class);
    private static final ContextKey<Integer> B = ContextKey.named("b", Integer.class);

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
    void capture_replay() {
        ContextCarrier.set(A, "alpha");
        ContextSnapshot snap = ContextSnapshot.capture();

        snap.replay(() ->
                assertThat(ContextCarrier.get(A)).isEqualTo("alpha"));
    }

    @Test
    void isEmpty_trueWhenNoKeys() {
        assertThat(ContextSnapshot.capture().isEmpty()).isTrue();
    }

    @Test
    void isEmpty_falseWhenHasKeys() {
        ContextCarrier.set(A, "alpha");
        assertThat(ContextSnapshot.capture().isEmpty()).isFalse();
    }

    @Test
    void values_returnsMap() {
        ContextCarrier.set(A, "alpha");
        ContextCarrier.set(B, 7);
        ContextSnapshot snap = ContextSnapshot.capture();
        assertThat(snap.values()).hasSize(2);
    }

    @Test
    void scopedRun_addsKey() {
        ContextCarrier.set(A, "base");
        ContextSnapshot snap = ContextSnapshot.capture();

        snap.scopedRun(A, "override", () -> {
            assertThat(ContextCarrier.get(A)).isEqualTo("override");
        });
        assertThat(ContextCarrier.get(A)).isEqualTo("base");
    }

    @Test
    void scopedRun_removesKeyWithValueNull() {
        ContextCarrier.set(A, "present");
        ContextSnapshot snap = ContextSnapshot.capture();

        snap.scopedRun(A, (String) null, () -> {
            assertThat(ContextCarrier.get(A)).isNull();
        });
        assertThat(ContextCarrier.get(A)).isEqualTo("present");
    }

    @Test
    void replay_nullTask_throws() {
        ContextSnapshot snap = ContextSnapshot.capture();
        assertThatThrownBy(() -> snap.replay(null))
                .isInstanceOf(NullPointerException.class);
    }
}