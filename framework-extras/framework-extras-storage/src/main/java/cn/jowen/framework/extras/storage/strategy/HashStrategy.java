package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 内容哈希策略：以内容 MD5 摘要作为对象键，天然实现「同内容只存一份」。
 *
 * <p>摘要前两位作为一级目录（{@code ab/cdef...}），避免单目录下文件过多。
 * 当 {@code content} 为 {@code null} 时降级为 {@link UuidStrategy}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class HashStrategy implements ObjectNameStrategy {

    /** fallback 不可变字段。 */
    private final UuidStrategy fallback = new UuidStrategy();

    /**
     * 执行generate操作。
     * @param originalName 参数 originalName
     * @return 结果
     */
    @Override
    public String generate(String originalName, @Nullable byte[] content) {
        if (content == null || content.length == 0) {
            return fallback.generate(originalName, content);
        }
        String digest = md5Hex(content);
        return digest.substring(0, 2) + "/" + digest + ObjectNames.extensionOf(originalName);
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public NamingStrategy type() {
        return NamingStrategy.HASH;
    }

    private static String md5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 为 JDK 强制实现，理论上不可达
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    /** @return 字符串内容的 MD5 摘要（便于测试与调试） */
    static String md5Hex(String text) {
        return md5Hex(text.getBytes(StandardCharsets.UTF_8));
    }
}
