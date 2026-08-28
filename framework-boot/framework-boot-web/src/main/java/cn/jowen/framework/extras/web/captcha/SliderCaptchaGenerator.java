package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 滑块验证码生成器：生成带缺口的背景图，答案为缺口横坐标。
 *
 * <p>前端需将用户拖动的距离与 {@link Captcha#code()}（缺口 x 坐标）比对，
 * 允许一定误差范围（由业务方设定，建议 ±5 像素）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SliderCaptchaGenerator implements CaptchaGenerator {

    /** 建议的校验容差（像素） */
    public static final int TOLERANCE = 5;

    /** width 不可变字段。 */
    private final int width;
    /** height 不可变字段。 */
    private final int height;
    /** blockSize 不可变字段。 */
    private final int blockSize;

    /**
     * 构造实例。
     * @param width 参数 width
     * @param height 参数 height
     * @param blockSize 参数 blockSize
     */
    public SliderCaptchaGenerator(int width, int height, int blockSize) {
        this.width = width;
        this.height = height;
        this.blockSize = blockSize;
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public ExtrasWebProperties.Captcha.CaptchaType type() {
        return ExtrasWebProperties.Captcha.CaptchaType.SLIDER;
    }

    /**
     * 执行generate操作。
     * @param expireSeconds 参数 expireSeconds
     * @return 结果
     */
    @Override
    public Captcha generate(long expireSeconds) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int gapX = rnd.nextInt(blockSize + 10, Math.max(blockSize + 11, width - blockSize - 10));
        int gapY = rnd.nextInt(10, Math.max(11, height - blockSize - 10));

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(220, 230, 240));
            g.fillRect(0, 0, width, height);
            for (int i = 0; i < 20; i++) {
                g.setColor(new Color(rnd.nextInt(180, 240), rnd.nextInt(180, 240), rnd.nextInt(180, 240)));
                g.fillOval(rnd.nextInt(width), rnd.nextInt(height), rnd.nextInt(20, 60), rnd.nextInt(20, 60));
            }
            // 缺口区域（浅色遮罩表示拼图块缺失位置）
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRect(gapX, gapY, blockSize, blockSize);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(gapX, gapY, blockSize, blockSize);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (java.io.IOException e) {
            throw new cn.jowen.framework.extras.common.exception.ExtrasException("生成滑块验证码失败", e);
        }
        return new Captcha(UUID.randomUUID().toString(), String.valueOf(gapX),
                "请拖动滑块拼合缺口", out.toByteArray(),
                System.currentTimeMillis() + expireSeconds * 1000L);
    }

    /**
     * 校验滑块位置是否在容差范围内。
     *
     * @param expectedX 缺口横坐标（字符串形式，来自 {@link Captcha#code()}）
     * @param actualX   用户拖动的横坐标
     * @return 是否通过
     */
    public static boolean validatePosition(String expectedX, int actualX) {
        try {
            int expected = Integer.parseInt(expectedX);
            return Math.abs(expected - actualX) <= TOLERANCE;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
