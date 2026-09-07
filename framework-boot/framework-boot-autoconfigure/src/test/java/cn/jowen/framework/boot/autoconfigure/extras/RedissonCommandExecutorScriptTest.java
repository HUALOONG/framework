package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.core.exception.BusinessException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedissonCommandExecutor} 脚本能力测试：验证 {@code supportsScript()} 与
 * {@code eval(...)} 对 Redisson {@link RScript} 的委托。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedissonCommandExecutorScriptTest {

    private static final String SCRIPT_TEXT = "return redis.call('incr', KEYS[1])";

    @Mock
    private RedissonClient client;

    @Mock
    private RScript script;

    /**
     * 桩化 {@code RScript#eval}。
     *
     * <p><b>注意</b>：Redisson 的 {@code eval} 末参是可变参数 {@code Object...}，
     * Mockito 的 {@code any()} 在可变参数位置<b>只匹配恰好一个元素</b>，
     * 因此必须按 ARGV 个数桩化，否则 0 个 / 多个参数的调用会落空返回 {@code null}。
     *
     * @param result   脚本返回值，可为 {@code null}
     * @param argCount ARGV 个数
     */
    private void stubEval(@Nullable Object result, int argCount) {
        switch (argCount) {
            case 0 -> when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(),
                    eq(RScript.ReturnType.VALUE), anyList())).thenReturn(result);
            case 1 -> when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(),
                    eq(RScript.ReturnType.VALUE), anyList(), any())).thenReturn(result);
            case 2 -> when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(),
                    eq(RScript.ReturnType.VALUE), anyList(), any(), any())).thenReturn(result);
            default -> when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(),
                    eq(RScript.ReturnType.VALUE), anyList(), any(), any(), any())).thenReturn(result);
        }
    }

    private RedissonCommandExecutor executor() {
        when(client.getScript()).thenReturn(script);
        return new RedissonCommandExecutor(client);
    }

    @Test
    void supportsScript_returnsTrueBecauseRedissonNativelySupportsLua() {
        assertThat(executor().supportsScript()).isTrue();
    }

    @Test
    void eval_delegatesToRScriptWithReadWriteModeAndValueReturnType() {
        stubEval(7L, 1);

        RedissonCommandExecutor executor = executor();

        assertThat(executor.eval(SCRIPT_TEXT, List.of("rl:k"), List.of("3"))).isEqualTo(7L);
        verify(script).eval(eq(RScript.Mode.READ_WRITE), eq(SCRIPT_TEXT),
                eq(RScript.ReturnType.VALUE), eq(List.of("rl:k")), eq("3"));
    }

    @Test
    void eval_passesAllKeysAndArgsThrough() {
        stubEval("OK", 3);

        RedissonCommandExecutor executor = executor();

        assertThat(executor.eval(SCRIPT_TEXT, List.of("k1", "k2"), List.of("a1", "a2", "a3")))
                .isEqualTo("OK");
        verify(script).eval(eq(RScript.Mode.READ_WRITE), eq(SCRIPT_TEXT),
                eq(RScript.ReturnType.VALUE), eq(List.of("k1", "k2")), eq("a1"), eq("a2"), eq("a3"));
    }

    @Test
    void eval_withEmptyKeysAndArgs_isAllowed() {
        stubEval(1L, 0);

        RedissonCommandExecutor executor = executor();

        assertThat(executor.eval(SCRIPT_TEXT, List.of(), List.of())).isEqualTo(1L);
        verify(script).eval(eq(RScript.Mode.READ_WRITE), eq(SCRIPT_TEXT),
                eq(RScript.ReturnType.VALUE), eq(List.<Object>of()));
    }

    @Test
    void eval_whenRedisReturnsNull_doesNotThrowNpeAndReturnsNull() {
        stubEval(null, 1);

        RedissonCommandExecutor executor = executor();

        // Redis 脚本无返回值时 eval 结果为 null，此处必须原样透传而非解引用
        assertThat(executor.eval(SCRIPT_TEXT, List.of("k"), List.of())).isNull();
        verify(script).eval(eq(RScript.Mode.READ_WRITE), eq(SCRIPT_TEXT),
                eq(RScript.ReturnType.VALUE), eq(List.of("k")));
    }

    @Test
    void eval_withBlankScript_rejectedByAssertion() {
        RedissonCommandExecutor executor = executor();

        assertThatThrownBy(() -> executor.eval("   ", List.of("k"), List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("script must not be blank");
    }

    @Test
    void eval_withNullScript_rejectedByAssertion() {
        RedissonCommandExecutor executor = executor();

        @SuppressWarnings("null")
        String nullScript = null;
        assertThatThrownBy(() -> executor.eval(nullScript, List.of("k"), List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("script must not be blank");
    }

    @Test
    void eval_withNullKeys_rejectedByAssertion() {
        RedissonCommandExecutor executor = executor();

        assertThatThrownBy(() -> executor.eval(SCRIPT_TEXT, null, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("keys must not be null");
    }

    @Test
    void eval_withNullArgs_rejectedByAssertion() {
        RedissonCommandExecutor executor = executor();

        assertThatThrownBy(() -> executor.eval(SCRIPT_TEXT, List.of("k"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("args must not be null");
    }
}
