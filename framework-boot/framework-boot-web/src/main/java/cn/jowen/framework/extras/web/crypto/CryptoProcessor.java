package cn.jowen.framework.extras.web.crypto;

import org.jspecify.annotations.NullMarked;

/**
 * 字段加解密处理器抽象。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface CryptoProcessor {

    /**
     * 解密入站密文。
     *
     * @param ciphertext 密文（base64）
     * @param keyAlias   密钥别名
     * @return 明文
     */
    String decrypt(String ciphertext, String keyAlias);

    /**
     * 加密出站明文。
     *
     * @param plaintext 明文
     * @param keyAlias  密钥别名
     * @return 密文（base64）
     */
    String encrypt(String plaintext, String keyAlias);
}
