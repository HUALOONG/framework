package cn.jowen.framework.extras.web.crypto;

import cn.jowen.framework.extras.common.util.CryptoUtils;
import org.jspecify.annotations.NullMarked;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 AES-GCM 的 {@link CryptoProcessor} 实现，按密钥别名管理多密钥。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class AesGcmCryptoProcessor implements CryptoProcessor {

    private final Map<String, byte[]> keys = new ConcurrentHashMap<>();
    private final String defaultAlias;

    public AesGcmCryptoProcessor(String defaultAlias, Map<String, byte[]> keys) {
        this.defaultAlias = defaultAlias;
        this.keys.putAll(keys);
    }

    @Override
    public String decrypt(String ciphertext, String keyAlias) {
        return CryptoUtils.decrypt(resolveKey(keyAlias), ciphertext);
    }

    @Override
    public String encrypt(String plaintext, String keyAlias) {
        return CryptoUtils.encrypt(resolveKey(keyAlias), plaintext);
    }

    private byte[] resolveKey(String keyAlias) {
        byte[] key = keys.getOrDefault(keyAlias, keys.get(defaultAlias));
        if (key == null) {
            throw new IllegalArgumentException("未找到加解密密钥: " + keyAlias);
        }
        return key;
    }
}
