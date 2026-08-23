package cn.jowen.framework.extras.notification;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地通知服务：按渠道路由到已注册的 {@link NotificationChannelHandler}。
 *
 * <p>零外部依赖，可直接 {@code new LocalNotificationService(List.of(new ConsoleNotificationHandler()))} 使用。
 * 未注册渠道返回 {@code success=false}（不抛异常）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class LocalNotificationService implements NotificationService {

    private final Map<NotificationChannel, NotificationChannelHandler> handlers;
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public LocalNotificationService() {
        this(List.of());
    }

    public LocalNotificationService(Collection<NotificationChannelHandler> handlers) {
        Objects.requireNonNull(handlers, "handlers must not be null");
        Map<NotificationChannel, NotificationChannelHandler> map = new java.util.EnumMap<>(NotificationChannel.class);
        for (NotificationChannelHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            for (NotificationChannel channel : NotificationChannel.values()) {
                if (handler.supports(channel)) {
                    map.put(channel, handler);
                }
            }
        }
        this.handlers = Map.copyOf(map);
    }

    @Override
    public NotificationResult send(NotificationRequest req) {
        Objects.requireNonNull(req, "req must not be null");
        NotificationChannelHandler handler = handlers.get(req.channel());
        if (handler == null) {
            return NotificationResult.failure(req.channel(), "未注册渠道处理器: " + req.channel());
        }
        try {
            return handler.send(req);
        } catch (RuntimeException e) {
            return NotificationResult.failure(req.channel(), "发送异常: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<NotificationResult> sendAsync(NotificationRequest req) {
        Objects.requireNonNull(req, "req must not be null");
        return CompletableFuture.supplyAsync(() -> send(req), asyncExecutor);
    }

    @Override
    public List<NotificationResult> sendBatch(List<NotificationRequest> reqs) {
        Objects.requireNonNull(reqs, "reqs must not be null");
        List<NotificationResult> results = new ArrayList<>(reqs.size());
        for (NotificationRequest req : reqs) {
            results.add(send(req));
        }
        return results;
    }

    /**
     * 关闭异步线程池（可选清理钩子）。
     */
    public void shutdown() {
        asyncExecutor.shutdown();
    }

    /**
     * 已注册的处理器集合（不可变）。
     */
    public Map<NotificationChannel, NotificationChannelHandler> handlers() {
        return handlers;
    }
}
