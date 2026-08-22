package cn.jowen.framework.boot.autoconfigure.logger;

import cn.jowen.framework.core.lifecycle.InitializingBean;
import cn.jowen.framework.logger.adapter.LoggerAdapter;
import cn.jowen.framework.logger.config.LoggerProperties;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

/**
 * 日志引导组件：在容器启动早期预热门面 → 底层适配器解析，确保业务代码首次取 logger 即可用。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class LoggerBootstrap implements InitializingBean {

    private final LoggerProperties properties;

    public LoggerBootstrap(LoggerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.isEnabled()) {
            return;
        }
        // 预热适配器解析：触发 SPI 查找默认实现（Logback / Log4j2）
        LoggerAdapter adapter = LoggerFactory.resolveAdapter();
        Logger log = LoggerFactory.getLogger(LoggerBootstrap.class);
        log.info("framework-logger 已就绪，底层适配器={}", adapter.getClass().getSimpleName());
    }
}
