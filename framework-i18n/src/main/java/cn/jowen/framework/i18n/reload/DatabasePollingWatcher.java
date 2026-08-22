package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 数据库轮询监听器：以固定间隔查询消息表变更指纹（版本号/更新时间最大值），
 * 指纹变化即触发 {@link ReloadableMessageSource#reload()}。
 *
 * <p>默认指纹 SQL 为 {@code SELECT MAX(version) FROM i18n_message}，可通过构造参数覆盖；
 * 消息表需含版本列（{@code version} 或 {@code updated_at}），每次变更递增。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class DatabasePollingWatcher implements ResourceWatcher {

    private final JdbcTemplate jdbcTemplate;
    private final String fingerprintSql;
    private final ReloadableMessageSource target;
    private final ResourceReloader reloader;
    private final Duration interval;

    private volatile boolean running;
    private volatile @Nullable ScheduledExecutorService executor;
    private volatile @Nullable Object lastFingerprint;

    public DatabasePollingWatcher(JdbcTemplate jdbcTemplate, String fingerprintSql,
                                  ReloadableMessageSource target, ResourceReloader reloader,
                                  Duration interval) {
        this.jdbcTemplate = jdbcTemplate;
        this.fingerprintSql = fingerprintSql;
        this.target = target;
        this.reloader = reloader;
        this.interval = interval;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        lastFingerprint = queryFingerprint();
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "i18n-db-poll");
            t.setDaemon(true);
            return t;
        });
        this.executor = service;
        service.scheduleWithFixedDelay(this::poll, interval.toMillis(), interval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        ScheduledExecutorService service = executor;
        if (service != null) {
            service.shutdownNow();
        }
        executor = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void poll() {
        if (!running) {
            return;
        }
        try {
            Object current = queryFingerprint();
            if (current == null) {
                return;
            }
            if (lastFingerprint == null || !current.equals(lastFingerprint)) {
                lastFingerprint = current;
                reloader.reload(target, null);
            }
        } catch (RuntimeException ignored) {
            // 轮询失败静默，等待下个周期重试
        }
    }

    private @Nullable Object queryFingerprint() {
        try {
            return jdbcTemplate.queryForObject(fingerprintSql, Object.class);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
