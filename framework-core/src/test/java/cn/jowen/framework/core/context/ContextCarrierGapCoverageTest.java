package cn.jowen.framework.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ContextCarrier#set} 在 ScopedValue 模式下的行为测试，补齐 JaCoCo 缺口行 86–95。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>{@code set()} 在 {@code runWith()} 作用域内、值非 null → put；</li>
 *   <li>{@code set()} 在 {@code runWith()} 作用域内、值 null → remove；</li>
 *   <li>{@code set()} 在 {@code runWith()} 作用域外 → IllegalStateException。</li>
 * </ul>
 *
 * <p>仅在 ScopedValue 可用（JDK 22+ 或 JDK 21 启用预览）时执行；
 * JDK 21 未启用预览时 {@link ScopedValueBridge#available()} 返回 false，
 * {@code useScopedValue()} 恒为 false，缺口行不可达。
 *
 * @author Yan
 * @since 0.0.1
 * @version 0.0.1
 */
@EnabledIf("scopedValueAvailable")
class ContextCarrierGapCoverageTest {

    private static final ContextKey<String> TENANT = ContextKey.named("gap-tenant", String.class);
    private static final ContextKey<Integer> LEVEL = ContextKey.named("gap-level", Integer.class);

    private final ContextCarrier.Mode originalMode = ContextCarrier.mode();

    static boolean scopedValueAvailable() {
        return ScopedValueBridge.available();
    }

    @BeforeEach
    void setUp() {
        ContextCarrier.configure(ContextCarrier.Mode.SCOPED_VALUE);
    }

    @AfterEach
    void tearDown() {
        ContextCarrier.configure(originalMode);
    }

    @Test
    void set_insideRunWith_nonNullValue_puts() {
        ContextCarrier.runWith(TENANT, "initial", () -> {
            ContextCarrier.set(TENANT, "updated");
            assertThat(ContextCarrier.get(TENANT)).isEqualTo("updated");
        });
    }

    @Test
    void set_insideRunWith_nullValue_removes() {
        ContextCarrier.runWith(TENANT, "existing", () -> {
            ContextCarrier.set(TENANT, (String) null);
            assertThat(ContextCarrier.get(TENANT)).isNull();
        });
    }

    @Test
    void set_insideRunWith_secondKey_puts() {
        ContextCarrier.runWith(TENANT, "t1", () -> {
            ContextCarrier.set(LEVEL, 42);
            assertThat(ContextCarrier.get(LEVEL)).isEqualTo(42);
        });
    }

    @Test
    void set_outsideRunWith_throwsIllegalState() {
        assertThatThrownBy(() -> ContextCarrier.set(TENANT, "outside"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("runWith");
    }

    @Test
    void set_outsideRunWith_nullValue_alsoThrows() {
        assertThatThrownBy(() -> ContextCarrier.set(TENANT, (String) null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("runWith");
    }
}
