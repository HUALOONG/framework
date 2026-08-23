package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.data.mybatis.config.MybatisFlexProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * MyBatis Flex 配置属性绑定类。继承 data 模块的纯 POJO {@link MybatisFlexProperties}，通过
 * {@code @EnableConfigurationProperties} 注册到 Spring 容器，避免 framework-cache 反向依赖 Spring。
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.data.mybatis")
public class BootMybatisFlexProperties extends MybatisFlexProperties {
}
