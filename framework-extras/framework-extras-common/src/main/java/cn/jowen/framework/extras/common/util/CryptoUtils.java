package cn.jowen.framework.extras.common.util;

import org.jspecify.annotations.NullMarked;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 对称加解密工具。
 *
 * <p>采用 128 位密钥、12 字节随机 IV、GCM 认证加密，返回结果格式为 {@code base64(iv || ciphertext)}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class CryptoUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 128;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private CryptoUtils() {
    }

    public static SecretKey generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(KEY_SIZE);
            return generator.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("生成密钥失败", e);
        }
    }

    public static String encrypt(byte[] key, String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    public static String decrypt(byte[] key, String ciphertext) {
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            byte[] payload = new byte[decoded.length - IV_LENGTH];
            System.arraycopy(decoded, IV_LENGTH, payload, 0, payload.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(payload);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }
}
