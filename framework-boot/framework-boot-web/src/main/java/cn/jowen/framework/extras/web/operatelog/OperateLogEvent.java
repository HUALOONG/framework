package cn.jowen.framework.extras.web.operatelog;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 操作日志事件。
 *
 * @param operator   操作人标识（可为 {@code null}）
 * @param module     所属模块
 * @param operation  操作名称
 * @param method     目标方法签名
 * @param params     入参快照（未开启记录时为 {@code null}）
 * @param result     返回值快照（未开启记录或异常时为 {@code null}）
 * @param success    是否成功
 * @param error      失败信息（成功时为 {@code null}）
 * @param costMillis 耗时（毫秒）
 * @param timestamp  发生时间
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record OperateLogEvent(
        @Nullable String operator,
        String module,
        String operation,
        String method,
        @Nullable String params,
        @Nullable String result,
        boolean success,
        @Nullable String error,
        long costMillis,
        Instant timestamp) {
}
