package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形验证码生成器：产出带干扰线的 PNG 图片。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class GraphicCaptchaGenerator implements CaptchaGenerator {

    /** CHARS 常量。 */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** width 不可变字段。 */
    private final int width;
    /** height 不可变字段。 */
    private final int height;
    /** length 不可变字段。 */
    private final int length;

    /**
     * 构造实例。
     * @param width 参数 width
     * @param height 参数 height
     * @param length 参数 length
     */
    public GraphicCaptchaGenerator(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public ExtrasWebProperties.Captcha.CaptchaType type() {
        return ExtrasWebProperties.Captcha.CaptchaType.GRAPHIC;
    }

    /**
     * 执行generate操作。
     * @param expireSeconds 参数 expireSeconds
     * @return 结果
     */
    @Override
    public Captcha generate(long expireSeconds) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(rnd.nextInt(CHARS.length())));
        }
        String code = sb.toString();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (height * 0.7)));
            for (int i = 0; i < length; i++) {
                g.setColor(new Color(rnd.nextInt(150), rnd.nextInt(150), rnd.nextInt(150)));
                g.drawString(String.valueOf(code.charAt(i)),
                        (int) (width * (i + 0.2) / length), (int) (height * 0.8));
            }
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(rnd.nextInt(200), rnd.nextInt(200), rnd.nextInt(200)));
                g.drawLine(rnd.nextInt(width), rnd.nextInt(height),
                        rnd.nextInt(width), rnd.nextInt(height));
            }
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (java.io.IOException e) {
            throw new cn.jowen.framework.extras.common.exception.ExtrasException("生成图形验证码失败", e);
        }
        return new Captcha(UUID.randomUUID().toString(), code, null, out.toByteArray(),
                System.currentTimeMillis() + expireSeconds * 1000L);
    }
}
