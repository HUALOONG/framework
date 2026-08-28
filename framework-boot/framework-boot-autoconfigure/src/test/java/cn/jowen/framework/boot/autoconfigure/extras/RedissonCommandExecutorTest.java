package cn.jowen.framework.boot.autoconfigure.extras;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedissonCommandExecutor} 单元测试：验证 Redis 命令适配与释放锁脚本的原子删除语义。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedissonCommandExecutorTest {

    @Mock
    private RedissonClient client;

    @Mock
    private RScript script;

    @SuppressWarnings("unchecked")
    private RBucket<String> mockBucket() {
        return mock(RBucket.class);
    }

    @Test
    void setIfAbsentDelegatesToTrySetWithMillis() {
        RBucket<String> bucket = mockBucket();
        when(client.<String>getBucket("lock:order")).thenReturn(bucket);
        when(bucket.trySet("v1", 30_000L, TimeUnit.MILLISECONDS)).thenReturn(true);

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);

        assertThat(executor.setIfAbsent("lock:order", "v1", 30_000L)).isTrue();
        verify(bucket).trySet("v1", 30_000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void setIfAbsentReturnsFalseWhenKeyExists() {
        RBucket<String> bucket = mockBucket();
        when(client.<String>getBucket("lock:order")).thenReturn(bucket);
        when(bucket.trySet(anyString(), eq(1_000L), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);

        assertThat(executor.setIfAbsent("lock:order", "v1", 1_000L)).isFalse();
    }

    @Test
    void getReturnsStoredValue() {
        RBucket<String> bucket = mockBucket();
        when(client.<String>getBucket("k")).thenReturn(bucket);
        when(bucket.get()).thenReturn("stored");

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);

        assertThat(executor.get("k")).isEqualTo("stored");
    }

    @Test
    void getReturnsNullWhenAbsent() {
        RBucket<String> bucket = mockBucket();
        when(client.<String>getBucket("missing")).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);

        assertThat(executor.get("missing")).isNull();
    }

    @Test
    void deleteDelegatesToBucket() {
        RBucket<String> bucket = mockBucket();
        when(client.<String>getBucket("k")).thenReturn(bucket);

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);
        executor.delete("k");

        verify(bucket).delete();
    }

    @Test
    void deleteIfMatchReturnsTrueWhenValueMatches() {
        when(client.getScript()).thenReturn(script);
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.LONG), anyList(), any()))
                .thenReturn(1L);

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);

        assertThat(executor.deleteIfMatch("lock:order", "v1")).isTrue();
    }

    @Test
    void deleteIfMatchReturnsFalseWhenValueMismatch() {
        when(client.getScript()).thenReturn(script);
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.LONG), anyList(), any()))
                .thenReturn(0L);

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);

        assertThat(executor.deleteIfMatch("lock:order", "other")).isFalse();
    }

    @Test
    void deleteIfMatchReturnsFalseWhenScriptReturnsNull() {
        when(client.getScript()).thenReturn(script);
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.LONG), anyList(), any()))
                .thenReturn(null);

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);

        assertThat(executor.deleteIfMatch("lock:order", "v1")).isFalse();
    }

    @Test
    void deleteIfMatchUsesLongReturnTypeForAtomicCompareAndDelete() {
        when(client.getScript()).thenReturn(script);
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.LONG), anyList(), any()))
                .thenReturn(1L);

        RedissonCommandExecutor executor = new RedissonCommandExecutor(client);
        executor.deleteIfMatch("k", "v");

        // 关键：del 返回整数，必须用 LONG 而非 VALUE，否则反序列化类型不匹配
        verify(script).eval(eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.LONG), eq(List.of("k")), eq("v"));
    }
}
