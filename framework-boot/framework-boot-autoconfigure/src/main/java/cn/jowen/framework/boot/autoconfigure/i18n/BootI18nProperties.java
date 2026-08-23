package cn.jowen.framework.boot.autoconfigure.i18n;

import cn.jowen.framework.i18n.config.I18nProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 国际化配置属性绑定类。继承 i18n 模块的纯 POJO {@link I18nProperties}，仅在 Boot 装配层标注
 * {@link ConfigurationProperties}，避免 framework-i18n 反向依赖 Spring（保持实现层零 Spring 依赖）。
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.i18n")
public class BootI18nProperties extends I18nProperties {
}
