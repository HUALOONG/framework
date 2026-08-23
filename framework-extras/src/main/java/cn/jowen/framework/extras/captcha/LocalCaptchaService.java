package cn.jowen.framework.extras.captcha;

import cn.jowen.framework.extras.config.CaptchaProperties;
import cn.jowen.framework.extras.captcha.generator.ArithmeticCaptchaGenerator;
import cn.jowen.framework.extras.captcha.generator.CaptchaGenerator;
import cn.jowen.framework.extras.captcha.generator.CaptchaImage;
import cn.jowen.framework.extras.captcha.generator.ImageCaptchaGenerator;
import cn.jowen.framework.extras.captcha.store.CaptchaStore;
import cn.jowen.framework.extras.captcha.store.LocalCaptchaStore;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 本地验证码服务：组合生成器、存储与配置，生成图形/算术验证码并校验。
 *
 * <p>零外部依赖，可直接 {@code new LocalCaptchaService(new LocalCaptchaStore())} 使用。
 * {@link CaptchaType#SLIDER} / {@link CaptchaType#SMS} 调用 {@link #generate(CaptchaType)}
 * 抛 {@link UnsupportedOperationException}（本轮未实现）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class LocalCaptchaService implements CaptchaService {

    private static final String DATA_URI_PREFIX = "data:image/png;base64,";

    private final CaptchaStore store;
    private final CaptchaProperties properties;
    private final Map<CaptchaType, CaptchaGenerator> generators;

    public LocalCaptchaService(@Nullable CaptchaStore store) {
        this(store, new CaptchaProperties());
    }

    public LocalCaptchaService(@Nullable CaptchaStore store, @Nullable CaptchaProperties properties) {
        this.store = store == null ? new LocalCaptchaStore() : store;
        this.properties = properties == null ? new CaptchaProperties() : properties;
        Map<CaptchaType, CaptchaGenerator> map = new EnumMap<>(CaptchaType.class);
        map.put(CaptchaType.IMAGE, new ImageCaptchaGenerator(this.properties));
        map.put(CaptchaType.ARITHMETIC, new ArithmeticCaptchaGenerator(this.properties));
        this.generators = Map.copyOf(map);
    }

    private static String encode(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", baos)) {
                throw new CaptchaException("不支持的图片格式: png");
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new CaptchaException("验证码图片编码失败", e);
        }
    }

    @Override
    public CaptchaResult generate(CaptchaType type) {
        Objects.requireNonNull(type, "type must not be null");
        CaptchaGenerator generator = generators.get(type);
        if (generator == null) {
            throw new UnsupportedOperationException("本轮未实现: " + type);
        }
        CaptchaImage captchaImage = generator.generate();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        store.put(captchaId, captchaImage.answer(), properties.getTtlMillis());
        return new CaptchaResult(captchaId, DATA_URI_PREFIX + encode(captchaImage.image()),
                properties.getTtlMillis(), Map.of());
    }

    @Override
    public boolean verify(String captchaId, String code) {
        return verify(captchaId, code, true);
    }

    @Override
    public boolean verify(@Nullable String captchaId, @Nullable String code, boolean deleteAfter) {
        if (captchaId == null || code == null) {
            return false;
        }
        String stored = store.get(captchaId);
        if (stored == null) {
            return false;
        }
        boolean matched = properties.isCaseSensitive() ? stored.equals(code) : stored.equalsIgnoreCase(code);
        if (matched && deleteAfter) {
            store.remove(captchaId);
        }
        return matched;
    }
}
