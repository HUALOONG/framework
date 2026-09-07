package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RedisCommandExecutor} 新增 default 方法的兼容性测试。
 *
 * <p><b>核心验证点</b>：
 * <ol>
 *   <li>只实现原有 4 个方法的既有实现类，不重写任何新方法也能编译、运行 —— 证明非 breaking；</li>
 *   <li>{@code supportsScript()} 默认 {@code false}；</li>
 *   <li>{@code eval(...)} 默认抛 {@link UnsupportedOperationException}，且消息含实现类名；</li>
 *   <li>只重写 {@code eval} 而不重写 {@code supportsScript()} 时，{@code supportsScript()}
 *       仍为 {@code false} —— 证明「重写 eval 必须同步重写 supportsScript」这一契约确有约束力。</li>
 * </ol>
 *
 * <p><b>Mockito 陷阱（设计文档 §2.5）</b>：{@code mock(RedisCommandExecutor.class)} 会把
 * default 方法也 stub 成返回默认值（{@code false} / {@code null}）而<b>不</b>抛异常，
 * 因此本测试「不支持脚本」分支一律使用真实最小实现类；仅在
 * {@link #mockEvalMustBeExplicitlyToldToCallRealMethod()} 中显式
 * {@code thenCallRealMethod()} 来还原真实的 default 行为。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
class RedisCommandExecutorDefaultTest {

    /**
     * 只实现原有 4 个方法的最小实现类：模拟业务方既有的适配器（扩展前就存在的代码）。
     */
    private static class LegacyFourMethodExecutor implements RedisCommandExecutor {

        private final Map<String, String> store = new HashMap<>();

        @Override
        public boolean setIfAbsent(String key, String value, long expireMillis) {
            if (store.containsKey(key)) {
                return false;
            }
            store.put(key, value);
            return true;
        }

        @Override
        public @Nullable String get(String key) {
            return store.get(key);
        }

        @Override
        public void delete(String key) {
            store.remove(key);
        }

        @Override
        public boolean deleteIfMatch(String key, String value) {
            if (value.equals(store.get(key))) {
                store.remove(key);
                return true;
            }
            return false;
        }
    }

    /**
     * 只重写 {@code eval} 但<b>不</b>重写 {@code supportsScript()} 的不合规实现。
     */
    private static final class EvalOnlyExecutor extends LegacyFourMethodExecutor {

        @Override
        public @Nullable Object eval(String script, List<String> keys, List<String> args) {
            return 1L;
        }
    }

    /**
     * 同时重写 {@code eval} 与 {@code supportsScript()} 的合规实现。
     */
    private static final class ScriptCapableExecutor extends LegacyFourMethodExecutor {

        @Override
        public boolean supportsScript() {
            return true;
        }

        @Override
        public @Nullable Object eval(String script, List<String> keys, List<String> args) {
            return "ok";
        }
    }

    @Test
    void legacyFourMethodImpl_keepsWorkingWithoutTouchingNewMethods() {
        LegacyFourMethodExecutor executor = new LegacyFourMethodExecutor();

        assertThat(executor.setIfAbsent("k", "v", 1_000L)).isTrue();
        assertThat(executor.setIfAbsent("k", "v2", 1_000L)).isFalse();
        assertThat(executor.get("k")).isEqualTo("v");
        assertThat(executor.deleteIfMatch("k", "other")).isFalse();
        assertThat(executor.deleteIfMatch("k", "v")).isTrue();
        executor.setIfAbsent("d", "x", 1_000L);
        executor.delete("d");
        assertThat(executor.get("d")).isNull();
    }

    @Test
    void legacyFourMethodImpl_supportsScriptDefaultsToFalse() {
        assertThat(new LegacyFourMethodExecutor().supportsScript()).isFalse();
    }

    @Test
    void legacyFourMethodImpl_evalThrowsUnsupportedOperationExceptionWithClassName() {
        LegacyFourMethodExecutor executor = new LegacyFourMethodExecutor();

        UnsupportedOperationException thrown = assertThrows(
                UnsupportedOperationException.class,
                () -> executor.eval("return 1", List.of("k"), List.of()));

        assertThat(thrown).hasMessageContaining(LegacyFourMethodExecutor.class.getName())
                .hasMessageContaining("does not support Lua script execution");
    }

    @Test
    void evalOnlyOverride_supportsScriptStillFalseBecauseDefaultWins() {
        EvalOnlyExecutor executor = new EvalOnlyExecutor();

        assertThat(executor.supportsScript()).isFalse();
        assertThat(executor.eval("return 1", List.of(), List.of())).isEqualTo(1L);
    }

    @Test
    void scriptCapableImpl_reportsTrueAndExecutesScript() {
        ScriptCapableExecutor executor = new ScriptCapableExecutor();

        assertThat(executor.supportsScript()).isTrue();
        assertThat(executor.eval("return 'ok'", List.of(), List.of())).isEqualTo("ok");
    }

    @Test
    void mockitoDefaultAnswer_stubsDefaultMethodsWithoutThrowing() {
        RedisCommandExecutor mockExecutor = mock(RedisCommandExecutor.class);

        // 陷阱实证：Mockito 默认把 default 方法 stub 成返回值默认值，并不会走真实 default 实现
        assertThat(mockExecutor.supportsScript()).isFalse();
        assertThat(mockExecutor.eval("return 1", List.of(), List.of())).isNull();
    }

    @Test
    void mockEvalMustBeExplicitlyToldToCallRealMethod() {
        RedisCommandExecutor mockExecutor = mock(RedisCommandExecutor.class);
        when(mockExecutor.eval(any(), any(), any())).thenCallRealMethod();

        assertThatThrownBy(() -> mockExecutor.eval("return 1", List.of(), List.of()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support Lua script execution");
    }
}
