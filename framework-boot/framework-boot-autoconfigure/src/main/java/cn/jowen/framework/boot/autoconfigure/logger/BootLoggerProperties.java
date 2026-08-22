package cn.jowen.framework.boot.autoconfigure.logger;

import cn.jowen.framework.logger.config.LoggerProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志配置属性绑定类。继承 logger 模块的纯 POJO {@link LoggerProperties}，仅在 Boot 装配层标注
 * {@link ConfigurationProperties}，避免 framework-logger 反向依赖 Spring（保持实现层零 Spring 依赖）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.logger")
public class BootLoggerProperties extends LoggerProperties {
}
