package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DatabasePollingWatcher} 测试。
 */
class DatabasePollingWatcherTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ReloadableMessageSource target = mock(ReloadableMessageSource.class);
    private final AtomicInteger reloadCount = new AtomicInteger();
    private final ResourceReloader reloader = new ResourceReloader();
    private final String sql = "SELECT MAX(version) FROM i18n_message";

    DatabasePollingWatcherTest() {
        doAnswer(invocation -> {
            reloadCount.incrementAndGet();
            return null;
        }).when(target).reload();
    }

    @Test
    void start_recordsFingerprintAndRuns() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Object.class))).thenReturn("v1");
        DatabasePollingWatcher watcher = new DatabasePollingWatcher(jdbcTemplate, sql, target, reloader,
                Duration.ofSeconds(60));
        watcher.start();
        try {
            assertThat(watcher.isRunning()).isTrue();
            verify(jdbcTemplate).queryForObject(sql, Object.class);
        } finally {
            watcher.stop();
        }
        assertThat(watcher.isRunning()).isFalse();
    }

    @Test
    void start_isIdempotent() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Object.class))).thenReturn("v1");
        DatabasePollingWatcher watcher = new DatabasePollingWatcher(jdbcTemplate, sql, target, reloader,
                Duration.ofSeconds(60));
        watcher.start();
        watcher.start();
        try {
            assertThat(watcher.isRunning()).isTrue();
        } finally {
            watcher.stop();
        }
    }

    @Test
    void fingerprintChange_triggersReload() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Object.class))).thenReturn("v1");
        DatabasePollingWatcher watcher = new DatabasePollingWatcher(jdbcTemplate, sql, target, reloader,
                Duration.ofSeconds(60));
        watcher.start();
        try {
            // 相同指纹不触发
            poll(watcher);
            assertThat(reloadCount.get()).isZero();

            // 指纹变化触发
            when(jdbcTemplate.queryForObject(anyString(), eq(Object.class))).thenReturn("v2");
            poll(watcher);
            assertThat(reloadCount.get()).isEqualTo(1);

            // 再次相同不触发
            poll(watcher);
            assertThat(reloadCount.get()).isEqualTo(1);
        } finally {
            watcher.stop();
        }
    }

    @Test
    void queryFailure_swallowedSilently() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Object.class))).thenThrow(new RuntimeException("db down"));
        DatabasePollingWatcher watcher = new DatabasePollingWatcher(jdbcTemplate, sql, target, reloader,
                Duration.ofSeconds(60));
        watcher.start();
        try {
            // start 时指纹查询失败不抛异常
            assertThat(watcher.isRunning()).isTrue();
            poll(watcher);
            assertThat(reloadCount.get()).isZero();
        } finally {
            watcher.stop();
        }
    }

    @Test
    void stop_isIdempotent() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Object.class))).thenReturn("v1");
        DatabasePollingWatcher watcher = new DatabasePollingWatcher(jdbcTemplate, sql, target, reloader,
                Duration.ofSeconds(60));
        watcher.start();
        watcher.stop();
        watcher.stop();
        assertThat(watcher.isRunning()).isFalse();
    }

    private void poll(DatabasePollingWatcher watcher) throws Exception {
        Method method = DatabasePollingWatcher.class.getDeclaredMethod("poll");
        method.setAccessible(true);
        method.invoke(watcher);
    }
}
