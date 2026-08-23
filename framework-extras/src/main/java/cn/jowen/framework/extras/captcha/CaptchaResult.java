package cn.jowen.framework.extras.captcha;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * 验证码生成结果。
 *
 * @param captchaId   验证码唯一标识（用于后续校验）
 * @param imageBase64 图片 Base64 数据（含 {@code data:image/png;base64,} 前缀）
 * @param expiresIn   有效期（毫秒）
 * @param extra       扩展字段（SLIDER/SMS 等后续能力预留，本轮恒为空）
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record CaptchaResult(String captchaId, String imageBase64, long expiresIn,
                            @Nullable Map<String, Object> extra) {

    public CaptchaResult {
        Objects.requireNonNull(captchaId, "captchaId must not be null");
        if (captchaId.isBlank()) {
            throw new IllegalArgumentException("captchaId must not be blank");
        }
        Objects.requireNonNull(imageBase64, "imageBase64 must not be null");
        extra = extra == null ? Map.of() : extra;
    }
}
