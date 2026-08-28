package cn.jowen.framework.extras.captcha.generator;

import cn.jowen.framework.extras.config.CaptchaProperties;
import org.jspecify.annotations.NullMarked;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 滑块验证码生成器（基础版，JDK AWT 零依赖）。
 *
 * <p>随机生成缺口横坐标 {@code x}，绘制背景（含缺口）与可拖动拼块两图，
 * 答案即 {@code x}（字符串形式），由前端拖拽校验。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class SliderCaptchaGenerator implements CaptchaGenerator {

    private static final Color PIECE_COLOR = new Color(0, 120, 215);

    private final CaptchaProperties properties;

    public SliderCaptchaGenerator(CaptchaProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties must not be null");
        }
        this.properties = properties;
    }

    @Override
    public CaptchaImage generate() {
        int width = properties.getWidth();
        int height = properties.getHeight();
        int pieceSize = Math.max(20, Math.min(40, height - 10));
        int maxX = width - pieceSize - 1;
        // 确保 x 落在合法区间，避免拼块越界
        int x = pieceSize + ThreadLocalRandom.current().nextInt(Math.max(1, maxX - pieceSize + 1));
        int y = pieceSize + ThreadLocalRandom.current().nextInt(Math.max(1, height - 2 * pieceSize));

        BufferedImage background = drawBackground(width, height, x, y, pieceSize);
        BufferedImage piece = drawPiece(pieceSize);
        return new CaptchaImage(background, String.valueOf(x), piece, x);
    }

    private BufferedImage drawBackground(int width, int height, int x, int y, int pieceSize) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            // 随机浅色噪点增加辨识度，弱化直接读取坐标
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 300; i++) {
                g.setColor(new Color(200 + random.nextInt(56), 200 + random.nextInt(56), 200 + random.nextInt(56)));
                g.fillRect(random.nextInt(width), random.nextInt(height), 2, 2);
            }
            // 在缺口处镂空（涂背景色），与拼块形状一致
            g.setColor(Color.WHITE);
            g.fillRect(x, y, pieceSize, pieceSize);
        } finally {
            g.dispose();
        }
        return image;
    }

    private BufferedImage drawPiece(int pieceSize) {
        BufferedImage piece = new BufferedImage(pieceSize, pieceSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = piece.createGraphics();
        try {
            g.setColor(PIECE_COLOR);
            g.fillRect(0, 0, pieceSize, pieceSize);
            g.setColor(PIECE_COLOR.darker());
            g.drawRect(0, 0, pieceSize - 1, pieceSize - 1);
        } finally {
            g.dispose();
        }
        return piece;
    }
}