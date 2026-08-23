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
public class FlexEncryptProcessor implements ExtensionRegistry.Extension {

    private static final String ALGORITHM = "AES";
    private static volatile String key = "";

    @Override public String name() { return "encrypt"; }
    @Override public int order() { return 300; }

    public static void setDefaultKey(String base64Key) { key = base64Key; }

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

    @FunctionalInterface
    public interface FieldEncryptor {
        String encrypt(String plain);
        default String decrypt(String cipher) { throw new UnsupportedOperationException("decrypt not implemented"); }
    }
}
