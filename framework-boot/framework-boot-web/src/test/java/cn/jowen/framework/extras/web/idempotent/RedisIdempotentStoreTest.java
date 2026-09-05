package cn.jowen.framework.extras.web.idempotent;

import cn.jowen.framework.core.exception.BusinessException;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisIdempotentStore} 单元测试。
 *
 * <p>核心关注两点：委托是否按约定调用 {@link RedisCommandExecutor}（键前缀拼接、
 * TTL 单位换算），以及参数校验是否严格。不启动 Redis、不需要网络。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisIdempotentStoreTest {

    @Mock
    private RedisCommandExecutor executor;

    /** 首次写入成功时返回 {@code true}，业务侧应放行。 */
    @Test
    void tryMarkReturnsTrueWhenFirstWriteSucceeds() {
        RedisIdempotentStore store = new RedisIdempotentStore(executor);
        when(executor.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        boolean accepted = store.tryMark("order-1", 60, TimeUnit.SECONDS);

        assertThat(accepted).isTrue();
        verify(executor).setIfAbsent("idempotent:order-1", "1", 60_000L);
    }

    /** 键已存在时返回 {@code false}，业务侧应拒绝重复请求。 */
    @Test
    void tryMarkReturnsFalseWhenKeyAlreadyExists() {
        RedisIdempotentStore store = new RedisIdempotentStore(executor);
        when(executor.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(false);

        assertThat(store.tryMark("order-1", 60, TimeUnit.SECONDS)).isFalse();
    }

    /** 自定义前缀应原样拼在业务键之前。 */
    @Test
    void customKeyPrefixIsApplied() {
        RedisIdempotentStore store = new RedisIdempotentStore(executor, "my-app:idem:");
        when(executor.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        store.tryMark("k", 1, TimeUnit.HOURS);

        verify(executor).setIfAbsent("my-app:idem:k", "1", 3_600_000L);
    }

    /** 过期时间必须按传入的时间单位换算为毫秒，而不是硬编码秒。 */
    @Test
    void expireUnitIsConvertedToMillis() {
        RedisIdempotentStore store = new RedisIdempotentStore(executor);
        when(executor.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        store.tryMark("k", 2, TimeUnit.MINUTES);

        verify(executor).setIfAbsent("idempotent:k", "1", 120_000L);
    }

    /** 移除时应删除带前缀的真实键。 */
    @Test
    void removeDeletesPrefixedKey() {
        RedisIdempotentStore store = new RedisIdempotentStore(executor, "p:");

        store.remove("k");

        verify(executor).delete("p:k");
        verify(executor, never()).delete("k");
    }

    /** 注入 null 执行器属于装配错误，必须在构造期快速失败。 */
    @Test
    void constructorRejectsNullExecutor() {
        assertThatThrownBy(() -> new RedisIdempotentStore(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("executor");
    }

    /** 空前缀会导致键名粘连，必须拒绝。 */
    @Test
    void constructorRejectsBlankPrefix() {
        assertThatThrownBy(() -> new RedisIdempotentStore(executor, "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("keyPrefix");
    }

    /** null 键会导致无意义的 Redis 调用，必须拒绝。 */
    @Test
    void tryMarkRejectsNullKey() {
        RedisIdempotentStore store = new RedisIdempotentStore(executor);

        assertThatThrownBy(() -> store.tryMark(null, 60, TimeUnit.SECONDS))
                .isInstanceOf(BusinessException.class);
        verify(executor, never()).setIfAbsent(anyString(), anyString(), anyLong());
    }

    /** 非正过期时间会被 Redis 拒绝或产生语义错误（立即过期），必须拒绝。 */
    @Test
    void tryMarkRejectsNonPositiveExpire() {
        RedisIdempotentStore store = new RedisIdempotentStore(executor);

        assertThatThrownBy(() -> store.tryMark("k", 0, TimeUnit.SECONDS))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expire");
        verify(executor, never()).setIfAbsent(anyString(), anyString(), anyLong());
    }

    /** 移除 null 键属于编程错误，必须拒绝。 */
    @Test
    void removeRejectsNullKey() {
        RedisIdempotentStore store = new RedisIdempotentStore(executor);

        assertThatThrownBy(() -> store.remove(null)).isInstanceOf(BusinessException.class);
        verify(executor, never()).delete(anyString());
    }
}
