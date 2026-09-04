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

    @Test
    void name() {
        assertThat(processor.name()).isEqualTo("encrypt");
    }

    @Test
    void order() {
        assertThat(processor.order()).isEqualTo(300);
    }

    @Test
    void encrypt_invalidKey_returnsPlain() {
        FlexEncryptProcessor.setDefaultKey("not-a-valid-base64-key!!");
        assertThat(processor.encrypt("secret")).isEqualTo("secret");
    }

    @Test
    void decrypt_invalidKey_returnsCipherText() {
        FlexEncryptProcessor.setDefaultKey("not-a-valid-base64-key!!");
        assertThat(processor.decrypt("cipher")).isEqualTo("cipher");
    }

    @Test
    void encryptEntity_invalidKey_safeNoop() {
        FlexEncryptProcessor.setDefaultKey("not-a-valid-base64-key!!");
        SecretEntity entity = new SecretEntity("u1", "secret");
        processor.encryptEntity(entity);
        assertThat(entity.secret).isEqualTo("secret");
    }

    @Test
    void encryptEntity_recordComponent_catchesReflectionError() {
        FlexEncryptProcessor.setDefaultKey(dummyKey());
        EncRecord entity = new EncRecord("secret");
        // record components cannot be mutated reflectively -> swallowed, original preserved
        processor.encryptEntity(entity);
        assertThat(entity.secret()).isEqualTo("secret");
    }

    record EncRecord(@Encrypted String secret) {
    }

    @Test
    void fieldEncryptor_defaultDecryptUnsupported() {
        FlexEncryptProcessor.FieldEncryptor encryptor = plain -> plain; // only encrypt implemented
        assertThat(encryptor.encrypt("x")).isEqualTo("x");
        assertThatThrownBy(() -> encryptor.decrypt("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ===== 实体级加解密（P0-C2 fire 链依赖） =====

    @Test
    void isConfigured_reflectsKey() {
        FlexEncryptProcessor.setDefaultKey(null);
        assertThat(processor.isConfigured()).isFalse();
        FlexEncryptProcessor.setDefaultKey(dummyKey());
        assertThat(processor.isConfigured()).isTrue();
    }

    @Test
    void encryptEntity_decryptEntity_roundtrip() {
        FlexEncryptProcessor.setDefaultKey(dummyKey());
        SecretEntity entity = new SecretEntity("u1", "plaintext-secret");
        processor.encryptEntity(entity);
        assertThat(entity.secret).isNotEqualTo("plaintext-secret");
        assertThat(entity.secret).isNotBlank();

        processor.decryptEntity(entity);
        assertThat(entity.secret).isEqualTo("plaintext-secret");
    }

    @Test
    void encryptEntity_ignoresNonAnnotatedFields() {
        FlexEncryptProcessor.setDefaultKey(dummyKey());
        SecretEntity entity = new SecretEntity("visible-name", "secret");
        processor.encryptEntity(entity);
        assertThat(entity.name).isEqualTo("visible-name"); // 未标注，保持明文
        assertThat(entity.secret).isNotEqualTo("secret");
    }

    @Test
    void encryptEntity_safeNoop_whenUnconfigured() {
        FlexEncryptProcessor.setDefaultKey(null);
        SecretEntity entity = new SecretEntity("u1", "secret");
        processor.encryptEntity(entity);
        assertThat(entity.secret).isEqualTo("secret"); // 未配置：不加密
        processor.decryptEntity(entity);
        assertThat(entity.secret).isEqualTo("secret");
    }

    private static String dummyKey() {
        byte[] key = new byte[16];
        new java.security.SecureRandom().nextBytes(key);
        return java.util.Base64.getEncoder().encodeToString(key);
    }

    static final class SecretEntity {
        String name;
        @Encrypted String secret;

        SecretEntity(String name, String secret) {
            this.name = name;
            this.secret = secret;
        }
    }
}
