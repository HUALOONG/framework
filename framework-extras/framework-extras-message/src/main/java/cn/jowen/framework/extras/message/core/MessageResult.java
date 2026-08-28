package cn.jowen.framework.extras.message.core;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 消息发送结果。
 *
 * @param successful 是否成功
 * @param taskId     成功时的任务号（渠道返回），失败时为 {@code null}
 * @param error      失败时的错误信息，成功时为 {@code null}
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record MessageResult(boolean successful, @Nullable String taskId, @Nullable String error) {

    /** 成功结果（无任务号） */
    private static final MessageResult OK = new MessageResult(true, null, null);

    /** @return 无任务号的成功结果 */
    public static MessageResult success() {
        return OK;
    }

    /**
     * 带任务号的成功结果。
     *
     * @param taskId 任务号
     * @return 成功结果
     */
    public static MessageResult success(String taskId) {
        return new MessageResult(true, taskId, null);
    }

    /**
     * 失败结果。
     *
     * @param error 错误信息
     * @return 失败结果
     */
    public static MessageResult failure(String error) {
        return new MessageResult(false, null, error);
    }
}
