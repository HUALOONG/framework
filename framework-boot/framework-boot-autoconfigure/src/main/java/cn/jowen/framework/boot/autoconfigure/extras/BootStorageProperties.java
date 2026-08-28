package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.properties.StorageProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置属性绑定类。继承 storage 模块的纯 POJO {@link StorageProperties}，仅在 Boot 装配层标注
 * {@link ConfigurationProperties}，避免 framework-extras 反向依赖 Spring（保持实现层零 Spring 依赖）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.extras.storage")
public class BootStorageProperties extends StorageProperties {
}
