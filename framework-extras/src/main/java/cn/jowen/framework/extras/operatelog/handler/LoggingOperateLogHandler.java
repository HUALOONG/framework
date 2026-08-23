package cn.jowen.framework.extras.operatelog.handler;

import cn.jowen.framework.extras.operatelog.OperateLogHandler;
import cn.jowen.framework.extras.operatelog.OperateLogRecord;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

/**
 * 默认操作日志处理器：将记录格式化后写入框架日志门面（{@link Logger}）。
 *
 * <p>零外部依赖，可直接用于本地调试；
 * 生产持久化可扩展 {@link OperateLogHandler} 自行实现。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class LoggingOperateLogHandler implements OperateLogHandler {

    private final Logger logger = LoggerFactory.getLogger(LoggingOperateLogHandler.class);

    private static String format(OperateLogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("[OPERATE-LOG] module=").append(record.module())
                .append(", action=").append(record.action())
                .append(", status=").append(record.status());
        if (record.operator() != null) {
            sb.append(", operator=").append(record.operator());
        }
        if (record.description() != null && !record.description().isEmpty()) {
            sb.append(", description=").append(record.description());
        }
        if (record.content() != null && !record.content().isEmpty()) {
            sb.append(", content=").append(record.content());
        }
        sb.append(", costTime=").append(record.costTime()).append("ms");
        return sb.toString();
    }

    @Override
    public void handle(OperateLogRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        logger.info(format(record));
    }
}
