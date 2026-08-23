package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.config.CacheProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存配置属性绑定类。继承 cache 模块的纯 POJO {@link CacheProperties}，仅在 Boot 装配层标注
 * {@link ConfigurationProperties}，避免 framework-cache 反向依赖 Spring（保持实现层零 Spring 依赖）。
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.cache")
public class BootCacheProperties extends CacheProperties {
}
