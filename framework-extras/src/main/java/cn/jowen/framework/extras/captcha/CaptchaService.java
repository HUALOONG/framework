package cn.jowen.framework.extras.captcha;

import org.jspecify.annotations.NullMarked;

/**
 * 验证码服务接口。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public interface CaptchaService {

    /**
     * 生成验证码。
     *
     * @param type 验证码类型
     * @return 生成结果（含图片 Base64 与标识）
     * @throws UnsupportedOperationException 当 {@code type} 为 {@link CaptchaType#SLIDER} 或
     *                                       {@link CaptchaType#SMS}（本轮未实现）
     */
    CaptchaResult generate(CaptchaType type);

    /**
     * 校验验证码（默认校验后删除）。
     *
     * @param captchaId 验证码标识
     * @param code      用户输入的验证码
     * @return 匹配返回 {@code true}；不存在/已过期/不匹配返回 {@code false}
     */
    boolean verify(String captchaId, String code);

    /**
     * 校验验证码（可控制是否删除）。
     *
     * @param captchaId   验证码标识
     * @param code        用户输入的验证码
     * @param deleteAfter 校验后是否删除（true 时一次性消费）
     * @return 匹配返回 {@code true}；不存在/已过期/不匹配返回 {@code false}
     */
    boolean verify(String captchaId, String code, boolean deleteAfter);
}
