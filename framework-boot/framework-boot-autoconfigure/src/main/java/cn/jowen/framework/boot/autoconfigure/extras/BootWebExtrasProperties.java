package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web 扩展配置属性绑定类。继承 web 模块的纯 POJO {@link ExtrasWebProperties}，仅在 Boot 装配层标注
 * {@link ConfigurationProperties}，避免 framework-extras 反向依赖 Spring（保持实现层零 Spring 依赖）。
 *
 * <p>嵌套类型（如 {@code ExtrasWebProperties.Captcha}）由 web 模块定义并被代码直接引用，
 * 故不在此重复声明；绑定器会沿继承链向上遍历父类字段完成赋值。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.extras.web")
public class BootWebExtrasProperties extends ExtrasWebProperties {
}
