package cn.jowen.framework.extras.notification;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 通知服务接口。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public interface NotificationService {

    /**
     * 同步发送。
     *
     * @param req 请求
     * @return 结果（未注册渠道返回 {@code success=false}，不抛异常）
     */
    NotificationResult send(NotificationRequest req);

    /**
     * 异步发送。
     *
     * @param req 请求
     * @return 完成后的结果 Future
     */
    CompletableFuture<NotificationResult> sendAsync(NotificationRequest req);

    /**
     * 批量发送（每条独立，部分失败不影响其他）。
     *
     * @param reqs 请求集合
     * @return 每条对应的结果列表
     */
    List<NotificationResult> sendBatch(List<NotificationRequest> reqs);
}
