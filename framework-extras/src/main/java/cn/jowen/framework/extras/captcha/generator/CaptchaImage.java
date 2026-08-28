package cn.jowen.framework.extras.captcha.generator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * 验证码图片封装。
 *
 * <p>普通验证码仅使用 {@code image}/{@code answer}；滑块验证码额外携带
 * {@code pieceImage}（可拖动拼块）与 {@code x}（缺口横坐标），供前端拖拽拼合校验。
 *
 * @param image       渲染好的背景图片
 * @param answer      标准答案（普通验证码为字符；滑块为 {@code x} 的字符串形式）
 * @param pieceImage  滑块拼块图片（普通验证码为 {@code null}）
 * @param x           滑块缺口横坐标（普通验证码为 0）
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record CaptchaImage(BufferedImage image, String answer,
                           @Nullable BufferedImage pieceImage, int x) {

    /**
     * 普通验证码便捷构造（无拼块、坐标 0）。
     *
     * @param image  渲染好的图片
     * @param answer 标准答案
     */
    public CaptchaImage(BufferedImage image, String answer) {
        this(image, answer, null, 0);
    }

    public CaptchaImage {
        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(answer, "answer must not be null");
    }
}