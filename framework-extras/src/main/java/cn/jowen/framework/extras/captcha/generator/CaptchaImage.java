package cn.jowen.framework.extras.captcha.generator;

import org.jspecify.annotations.NullMarked;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * 验证码图片封装。
 *
 * @param image  渲染好的图片
 * @param answer 标准答案（校验依据）
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record CaptchaImage(BufferedImage image, String answer) {

    public CaptchaImage {
        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(answer, "answer must not be null");
    }
}
