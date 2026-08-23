package cn.jowen.framework.extras.operatelog;

import org.jspecify.annotations.NullMarked;

/**
 * 操作日志处理器（SPI）：消费一条 {@link OperateLogRecord}。实现可为本地日志、数据库、消息队列等。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public interface OperateLogHandler {

    /**
     * 处理操作日志。
     *
     * @param record 日志记录（不可为 null）
     */
    void handle(OperateLogRecord record);
}
