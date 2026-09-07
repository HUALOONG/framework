package cn.jowen.framework.extras.web.lock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AbstractRedisCommandExecutorContractTest} 的合规实现验证：以
 * {@link FakeRedisCommandExecutor}（内存模拟、支持脚本）驱动，应全部契约通过。
 *
 * <p>同时以「故意不合规」的匿名实现覆盖 {@code verify()} 的失败分支（B1/B2/B3），
 * 保证契约器自身逻辑被正确执行而非恒绿。</p>
 */
class RedisCommandExecutorContractTest extends AbstractRedisCommandExecutorContractTest {

    @Override
    protected RedisCommandExecutor createExecutor() {
        return new FakeRedisCommandExecutor();
    }

    private static RedisCommandExecutor stub() {
        return new RedisCommandExecutor() {
            @Override
            public boolean setIfAbsent(String key, String value, long expireMillis) {
                return false;
            }

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void delete(String key) {
            }

            @Override
            public boolean deleteIfMatch(String key, String value) {
                return false;
            }
        };
    }

    @Test
    void inconsistent_supportsScriptTrueButEvalThrowsUoe_isReported() {
        RedisCommandExecutor bad = new RedisCommandExecutor() {
            @Override
            public boolean setIfAbsent(String key, String value, long expireMillis) {
                return false;
            }

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void delete(String key) {
            }

            @Override
            public boolean deleteIfMatch(String key, String value) {
                return false;
            }

            @Override
            public boolean supportsScript() {
                return true;
            }

            @Override
            public Object eval(String script, List<String> keys, List<String> args) {
                throw new UnsupportedOperationException("not really supported");
            }
        };
        assertThat(verify(bad)).anyMatch(f -> f.contains("B1"));
    }

    @Test
    void inconsistent_supportsScriptFalseButEvalNotThrowing_isReported() {
        RedisCommandExecutor bad = new RedisCommandExecutor() {
            @Override
            public boolean setIfAbsent(String key, String value, long expireMillis) {
                return false;
            }

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void delete(String key) {
            }

            @Override
            public boolean deleteIfMatch(String key, String value) {
                return false;
            }

            @Override
            public boolean supportsScript() {
                return false;
            }

            @Override
            public Object eval(String script, List<String> keys, List<String> args) {
                return 1L;
            }
        };
        assertThat(verify(bad)).anyMatch(f -> f.contains("B2"));
    }

    @Test
    void nullScriptReturningNull_isReported() {
        RedisCommandExecutor bad = new RedisCommandExecutor() {
            @Override
            public boolean setIfAbsent(String key, String value, long expireMillis) {
                return false;
            }

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void delete(String key) {
            }

            @Override
            public boolean deleteIfMatch(String key, String value) {
                return false;
            }

            @Override
            public Object eval(String script, List<String> keys, List<String> args) {
                return null;
            }
        };
        assertThat(verify(bad)).anyMatch(f -> f.contains("B3"));
    }
}
