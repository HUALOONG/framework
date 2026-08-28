package cn.jowen.framework.extras.web.captcha;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 验证码内容。
 *
 * @param id       验证码标识（用于后续校验）
 * @param code     明文答案（不应对外暴露）
 * @param text     展示给用户的题目或提示（如算术题面）
 * @param image    图形验证码的 PNG 字节，非图形类型为 {@code null}
 * @param expireAt 过期时间戳（毫秒）
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record Captcha(String id, String code, @Nullable String text,
                      @Nullable byte[] image, long expireAt) {

    /** @return 是否已过期 */
    public boolean expired() {
        return System.currentTimeMillis() > expireAt;
    }

    /** @return 过期时间的 {@link Instant} 视图 */
    public Instant expireAtInstant() {
        return Instant.ofEpochMilli(expireAt);
    }
}
