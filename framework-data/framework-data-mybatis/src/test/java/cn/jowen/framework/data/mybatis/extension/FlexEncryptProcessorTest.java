package cn.jowen.framework.data.mybatis.extension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlexEncryptProcessorTest {

    private final FlexEncryptProcessor processor = new FlexEncryptProcessor();

    @Test
    void encrypt_decrypt_roundtrip() {
        // Generate a 16-byte AES key (Base64)
        byte[] key = new byte[16];
        new java.security.SecureRandom().nextBytes(key);
        String base64Key = java.util.Base64.getEncoder().encodeToString(key);
        FlexEncryptProcessor.setDefaultKey(base64Key);

        String plain = "Hello World 12345";
        String encrypted = processor.encrypt(plain);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plain);

        String decrypted = processor.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void encrypt_null() {
        assertThat(processor.encrypt(null)).isNull();
    }

    @Test
    void encrypt_empty() {
        assertThat(processor.encrypt("")).isEmpty();
    }

    @Test
    void decrypt_null() {
        assertThat(processor.decrypt(null)).isNull();
    }

    @Test
    void decrypt_empty() {
        assertThat(processor.decrypt("")).isEmpty();
    }

    @Test
    void encrypt_noKey_returnsPlain() {
        FlexEncryptProcessor.setDefaultKey(null);
        String result = processor.encrypt("test");
        assertThat(result).isEqualTo("test");
    }
}
