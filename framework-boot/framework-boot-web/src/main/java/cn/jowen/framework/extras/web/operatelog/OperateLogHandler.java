package cn.jowen.framework.extras.web.operatelog;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 操作日志处理器：决定日志落哪里（日志框架 / DB / MQ）。
 *
 * <p>默认实现输出结构化日志；用户可注入自定义实现进行持久化。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface OperateLogHandler {

    /**
     * 处理一条操作日志。
     *
     * @param event 日志事件
     */
    void handle(OperateLogEvent event);

    /**
     * 基于 SLF4J 的默认实现。
     */
    @NullMarked
    final class Slf4j implements OperateLogHandler {
        /** log 常量。 */
        private static final Logger log = LoggerFactory.getLogger(Slf4j.class);

        /**
         * 执行handle操作。
         * @param event 参数 event
         */
        @Override
        public void handle(OperateLogEvent event) {
            log.info("[OPERATE-LOG] module={} operation={} operator={} method={} success={} cost={}ms error={}",
                    event.module(), event.operation(), event.operator(), event.method(),
                    event.success(), event.costMillis(), event.error());
        }
    }
}
