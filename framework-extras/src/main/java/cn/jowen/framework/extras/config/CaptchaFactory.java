package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 验证码 Bean 工厂。
 *
 * <p>负责创建和配置验证码相关 Bean，由 boot-autoconfigure 调用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class CaptchaFactory {

    private final CaptchaProperties properties;

    public CaptchaFactory(CaptchaProperties properties) {
        this.properties = properties;
    }

    // TODO: 实现验证码 Bean 工厂方法
}
