package cn.jowen.framework.extras.captcha.generator;

import cn.jowen.framework.extras.config.CaptchaProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.Random;

/**
 * 图形验证码生成器：基于 JDK {@link BufferedImage} 渲染字符 + 干扰线 + 噪点。
 *
 * <p>零外部依赖，可直接 {@code new} 出来使用。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class ImageCaptchaGenerator implements CaptchaGenerator {

    private final CaptchaProperties properties;
    private final Random random = new SecureRandom();

    public ImageCaptchaGenerator() {
        this(new CaptchaProperties());
    }

    public ImageCaptchaGenerator(@Nullable CaptchaProperties properties) {
        this.properties = properties == null ? new CaptchaProperties() : properties;
    }

    @Override
    public CaptchaImage generate() {
        int width = properties.getWidth();
        int height = properties.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            // 背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            // 干扰线
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor());
                g.drawLine(random.nextInt(width), random.nextInt(height),
                        random.nextInt(width), random.nextInt(height));
            }

            // 字符
            String code = randomText();
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, height - 6));
            g.setFont(font);
            int step = width / (code.length() + 1);
            for (int i = 0; i < code.length(); i++) {
                g.setColor(randomColor());
                int x = step * (i + 1);
                int y = height / 2 + random.nextInt(height / 4);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
            }

            // 噪点
            for (int i = 0; i < 30; i++) {
                g.setColor(randomColor());
                g.fillRect(random.nextInt(width), random.nextInt(height), 1, 1);
            }
            return new CaptchaImage(image, code);
        } finally {
            g.dispose();
        }
    }

    private String randomText() {
        String chars = properties.getCharSet();
        if (chars.isEmpty()) {
            chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        }
        StringBuilder sb = new StringBuilder(properties.getLength());
        for (int i = 0; i < properties.getLength(); i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private Color randomColor() {
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
}
