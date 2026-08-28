package cn.jowen.framework.extras.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CryptoUtils} 测试。
 *
 * <p>覆盖：密钥生成、AES-GCM 加解密往返、IV 随机性、错误密钥/篡改密文/非法输入的失败路径。
 */
class CryptoUtilsTest {

    private static final int IV_LENGTH = 12;

    // ---------- generateKey ----------

    @Test
    void generateKey_produces128BitAesKey() {
        SecretKey key = CryptoUtils.generateKey();
        assertThat(key.getAlgorithm()).isEqualTo("AES");
        assertThat(key.getEncoded()).hasSize(16);
    }

    @Test
    void generateKey_producesDifferentKeysEachCall() {
        Set<String> encoded = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            encoded.add(Base64.getEncoder().encodeToString(CryptoUtils.generateKey().getEncoded()));
        }
        assertThat(encoded).hasSize(20);
    }

    // ---------- 加解密往返 ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "hello",
            "",
            "中文与 emoji \uD83D\uDE00 混排",
            "line1\nline2\tend",
            "0123456789012345678901234567890123456789012345678901234567890123456789"
    })
    void encryptThenDecrypt_roundTripsPlaintext(String plaintext) {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        String ciphertext = CryptoUtils.encrypt(key, plaintext);
        assertThat(CryptoUtils.decrypt(key, ciphertext)).isEqualTo(plaintext);
    }

    @Test
    void encrypt_outputIsBase64OfIvPlusCiphertext() {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        String ciphertext = CryptoUtils.encrypt(key, "payload");

        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        // 12 字节 IV + 密文 + 16 字节 GCM tag
        assertThat(decoded.length).isGreaterThan(IV_LENGTH + 16);
    }

    @Test
    void encrypt_producesDifferentCiphertextForSamePlaintext() {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        Set<String> ciphertexts = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            ciphertexts.add(CryptoUtils.encrypt(key, "same-plaintext"));
        }
        assertThat(ciphertexts).as("随机 IV 应使同一明文每次密文不同").hasSize(20);
    }

    @Test
    void decrypt_supportsRepeatedInvocationOnSameCiphertext() {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        String ciphertext = CryptoUtils.encrypt(key, "idempotent");
        assertThat(CryptoUtils.decrypt(key, ciphertext)).isEqualTo("idempotent");
        assertThat(CryptoUtils.decrypt(key, ciphertext)).isEqualTo("idempotent");
    }

    // ---------- 失败路径 ----------

    @Test
    void decrypt_failsWithWrongKey() {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        byte[] otherKey = CryptoUtils.generateKey().getEncoded();
        String ciphertext = CryptoUtils.encrypt(key, "secret");

        assertThatThrownBy(() -> CryptoUtils.decrypt(otherKey, ciphertext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void decrypt_failsWhenCiphertextTampered() {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        byte[] raw = Base64.getDecoder().decode(CryptoUtils.encrypt(key, "secret"));
        raw[raw.length - 1] ^= 0x01; // 翻转认证 tag 的一个 bit
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> CryptoUtils.decrypt(key, tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void decrypt_failsWhenIvTampered() {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        byte[] raw = Base64.getDecoder().decode(CryptoUtils.encrypt(key, "secret"));
        raw[0] ^= 0x01; // 破坏 IV
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> CryptoUtils.decrypt(key, tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void decrypt_failsOnInvalidBase64() {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        assertThatThrownBy(() -> CryptoUtils.decrypt(key, "not-base64!!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void decrypt_failsOnTooShortPayload() {
        byte[] key = CryptoUtils.generateKey().getEncoded();
        String tooShort = Base64.getEncoder().encodeToString(new byte[4]);
        assertThatThrownBy(() -> CryptoUtils.decrypt(key, tooShort))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void encrypt_failsOnInvalidKeyLength() {
        byte[] badKey = new byte[7];
        assertThatThrownBy(() -> CryptoUtils.encrypt(badKey, "text"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("加密失败");
    }

    @Test
    void encrypt_failuresCarryRootCause() {
        assertThatThrownBy(() -> CryptoUtils.encrypt(new byte[3], "text"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(Exception.class);
    }
}
