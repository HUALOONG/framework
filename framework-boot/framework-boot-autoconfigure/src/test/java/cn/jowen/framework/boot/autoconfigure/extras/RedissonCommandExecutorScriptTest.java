package cn.jowen.framework.boot.autoconfigure.extras;

import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RedissonCommandExecutor} 脚本能力契约验证（镜像 {@code AbstractRedisCommandExecutorContractTest} 的 B 组）。
 *
 * <p>由于 {@code RedissonCommandExecutor} 位于装配层且依赖 Redisson 客户端，无法复用 boot-web 的
 * 抽象契约基类（避免引入 test-jar 改造），故以动态代理 fake {@link RScript} 独立镜像同一套契约语义，
 * 确保真实适配器与框架契约一致。使用代理而非 Mockito 存根，是为规避 {@code RScript.eval} 多重
 * varargs 重载在 Mockito 匹配下的歧义。</p>
 */
class RedissonCommandExecutorScriptTest {

    private RedissonCommandExecutor newExecutor(RedissonClient client) {
        return new RedissonCommandExecutor(client);
    }

    /**
     * 构造一个仅拦截 {@code eval}、记录入参并返回固定值的 {@link RScript} 替身。
     *
     * @param evalReturn {@code eval} 的返回值（可为 {@code null}，模拟脚本无返回值）
     * @param captured   出参：最近一次 {@code eval} 的入参快照
     * @return RScript 替身
     */
    private static RScript fakeScript(Object evalReturn, AtomicReference<Object[]> captured) {
        return (RScript) Proxy.newProxyInstance(
                RScript.class.getClassLoader(),
                new Class<?>[]{RScript.class},
                (proxy, method, args) -> {
                    if ("eval".equals(method.getName()) && args != null && args.length >= 5) {
                        captured.set(args);
                        return evalReturn;
                    }
                    throw new UnsupportedOperationException("unexpected RScript call: " + method.getName());
                });
    }

    @Test
    void supportsScriptIsTrue() {
        assertThat(newExecutor(mock(RedissonClient.class)).supportsScript()).isTrue();
    }

    @Test
    void evalForwardsModeReturnTypeKeysAndArgsAndReturnsValue() {
        AtomicReference<Object[]> captured = new AtomicReference<>();
        RScript script = fakeScript(1L, captured);
        RedissonClient client = mock(RedissonClient.class);
        when(client.getScript()).thenReturn(script);

        RedissonCommandExecutor ex = newExecutor(client);
        Object result = ex.eval("return 1", List.of("k1"), List.of("a1", "a2"));

        assertThat(result).isEqualTo(1L);
        Object[] args = captured.get();
        assertThat(args).hasSize(5);
        assertThat(args[0]).isEqualTo(RScript.Mode.READ_WRITE);
        assertThat(args[1]).isEqualTo("return 1");
        assertThat(args[2]).isEqualTo(RScript.ReturnType.VALUE);
        @SuppressWarnings("unchecked")
        List<Object> keys = (List<Object>) args[3];
        assertThat(keys).containsExactly("k1");
        assertThat((Object[]) args[4]).containsExactly("a1", "a2");
    }

    @Test
    void evalNullReturnIsPassedThroughWithoutNpe() {
        AtomicReference<Object[]> captured = new AtomicReference<>();
        RScript script = fakeScript(null, captured);
        RedissonClient client = mock(RedissonClient.class);
        when(client.getScript()).thenReturn(script);

        RedissonCommandExecutor ex = newExecutor(client);
        assertThat(ex.eval("no return", List.of("k"), List.of("a"))).isNull();
        assertThat(captured.get()).hasSize(5);
    }

    @Test
    void evalNullScriptFailsFast() {
        RedissonCommandExecutor ex = newExecutor(mock(RedissonClient.class));
        assertThatThrownBy(() -> ex.eval(null, List.of("k"), List.of("a")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void evalBlankScriptFailsFast() {
        RedissonCommandExecutor ex = newExecutor(mock(RedissonClient.class));
        assertThatThrownBy(() -> ex.eval("   ", List.of("k"), List.of("a")))
                .isInstanceOf(RuntimeException.class);
    }
}
