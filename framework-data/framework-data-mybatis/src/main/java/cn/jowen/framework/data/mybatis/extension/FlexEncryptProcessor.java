package cn.jowen.framework.data.mybatis.extension;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
/**
 * 「FlexEncrypt」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexEncryptProcessor implements ExtensionRegistry.Extension {

    /** ALGORITHM 常量。 */
    private static final String ALGORITHM = "AES";
    /** key 静态变量。 */
    private static volatile String key = "";

    /** return 字段。 */
    @Override public String name() { return "encrypt"; }
    /** return 字段。 */
    @Override public int order() { return 300; }

    /** key 静态变量。 */
    public static void setDefaultKey(String base64Key) { key = base64Key; }

    /**
     * 执行encrypt操作。
     * @param plain 参数 plain
     * @return 结果
     */
    @Nullable
    public String encrypt(@Nullable String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        try {
            byte[] decodedKey = Base64.getDecoder().decode(resolveKey());
            SecretKeySpec spec = new SecretKeySpec(decodedKey, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM + "/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, spec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return plain;
        }
    }

    /**
     * 执行decrypt操作。
     * @param cipherText 参数 cipherText
     * @return 结果
     */
    @Nullable
    public String decrypt(@Nullable String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return cipherText;
        try {
            byte[] decodedKey = Base64.getDecoder().decode(resolveKey());
            SecretKeySpec spec = new SecretKeySpec(decodedKey, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM + "/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, spec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cipherText;
        }
    }

    private String resolveKey() {
        return key;
    }

    /** FieldEncryptor 字段。 */
    @FunctionalInterface
    /**
     * 「FieldEncryptor」接口定义。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public interface FieldEncryptor {
        String encrypt(String plain);
        /** not 字段。 */
        default String decrypt(String cipher) { throw new UnsupportedOperationException("decrypt not implemented"); }
    }
}
