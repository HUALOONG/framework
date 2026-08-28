package cn.jowen.framework.extras.web.crypto;

import cn.jowen.framework.extras.common.util.CryptoUtils;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AesGcmCryptoProcessor} 测试。
 *
 * <p>覆盖：加解密往返、多密钥别名路由、未知别名回退默认密钥、缺失密钥报错、
 * 跨别名解密失败、密文篡改失败、构造时对入参 Map 的拷贝语义。
 */
class AesGcmCryptoProcessorTest {

    private static byte[] key() {
        return CryptoUtils.generateKey().getEncoded();
    }

    private static AesGcmCryptoProcessor singleKeyProcessor() {
        return new AesGcmCryptoProcessor("default", Map.of("default", key()));
    }

    // ---------- 往返 ----------

    @Test
    void encryptThenDecrypt_roundTrips() {
        AesGcmCryptoProcessor processor = singleKeyProcessor();

        String ciphertext = processor.encrypt("13800000000", "default");
        assertThat(ciphertext).isNotEqualTo("13800000000");
        assertThat(processor.decrypt(ciphertext, "default")).isEqualTo("13800000000");
    }

    @Test
    void encryptThenDecrypt_handlesEmptyAndUnicode() {
        AesGcmCryptoProcessor processor = singleKeyProcessor();

        assertThat(processor.decrypt(processor.encrypt("", "default"), "default")).isEmpty();
        assertThat(processor.decrypt(processor.encrypt("张三·李四", "default"), "default"))
                .isEqualTo("张三·李四");
    }

    @Test
    void encrypt_producesDistinctCiphertextPerCall() {
        AesGcmCryptoProcessor processor = singleKeyProcessor();

        String first = processor.encrypt("same", "default");
        String second = processor.encrypt("same", "default");

        assertThat(first).isNotEqualTo(second);
        assertThat(processor.decrypt(first, "default")).isEqualTo("same");
        assertThat(processor.decrypt(second, "default")).isEqualTo("same");
    }

    @Test
    void ciphertext_isBase64Decodable() {
        String ciphertext = singleKeyProcessor().encrypt("payload", "default");

        assertThat(Base64.getDecoder().decode(ciphertext)).isNotEmpty();
    }

    // ---------- 多密钥别名 ----------

    @Test
    void multipleAliases_useTheirOwnKeys() {
        Map<String, byte[]> keys = Map.of("a", key(), "b", key());
        AesGcmCryptoProcessor processor = new AesGcmCryptoProcessor("a", keys);

        String withA = processor.encrypt("text", "a");
        String withB = processor.encrypt("text", "b");

        assertThat(processor.decrypt(withA, "a")).isEqualTo("text");
        assertThat(processor.decrypt(withB, "b")).isEqualTo("text");
    }

    @Test
    void crossAliasDecryption_fails() {
        Map<String, byte[]> keys = Map.of("a", key(), "b", key());
        AesGcmCryptoProcessor processor = new AesGcmCryptoProcessor("a", keys);
        String withA = processor.encrypt("text", "a");

        assertThatThrownBy(() -> processor.decrypt(withA, "b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void unknownAlias_fallsBackToDefaultKey() {
        byte[] defaultKey = key();
        AesGcmCryptoProcessor processor =
                new AesGcmCryptoProcessor("default", Map.of("default", defaultKey));

        // 未注册别名时回退默认密钥，因此与默认密钥加密的结果可互相解开
        String viaUnknown = processor.encrypt("text", "no-such-alias");
        assertThat(processor.decrypt(viaUnknown, "default")).isEqualTo("text");
        assertThat(processor.decrypt(processor.encrypt("text", "default"), "another-unknown"))
                .isEqualTo("text");
    }

    @Test
    void missingDefaultKey_throwsIllegalArgument() {
        AesGcmCryptoProcessor processor = new AesGcmCryptoProcessor("absent", Map.of("other", key()));

        assertThatThrownBy(() -> processor.encrypt("text", "nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未找到加解密密钥")
                .hasMessageContaining("nope");
    }

    @Test
    void emptyKeyMap_throwsOnAnyOperation() {
        AesGcmCryptoProcessor processor = new AesGcmCryptoProcessor("default", Map.of());

        assertThatThrownBy(() -> processor.encrypt("text", "default"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.decrypt("Zm9v", "default"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- 失败路径 ----------

    @Test
    void decrypt_failsOnTamperedCiphertext() {
        AesGcmCryptoProcessor processor = singleKeyProcessor();
        byte[] raw = Base64.getDecoder().decode(processor.encrypt("secret", "default"));
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> processor.decrypt(tampered, "default"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void decrypt_failsOnGarbageInput() {
        AesGcmCryptoProcessor processor = singleKeyProcessor();

        assertThatThrownBy(() -> processor.decrypt("!!!not base64!!!", "default"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void encrypt_failsOnInvalidKeyLength() {
        AesGcmCryptoProcessor processor =
                new AesGcmCryptoProcessor("bad", Map.of("bad", new byte[5]));

        assertThatThrownBy(() -> processor.encrypt("text", "bad"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("加密失败");
    }

    // ---------- 构造语义 ----------

    @Test
    void constructor_copiesKeyMap_soLaterMutationHasNoEffect() {
        Map<String, byte[]> source = new HashMap<>();
        source.put("default", key());
        AesGcmCryptoProcessor processor = new AesGcmCryptoProcessor("default", source);

        source.clear();
        source.put("injected", key());

        assertThat(processor.decrypt(processor.encrypt("text", "default"), "default")).isEqualTo("text");
    }

    @Test
    void implementsCryptoProcessorContract() {
        assertThat(singleKeyProcessor()).isInstanceOf(CryptoProcessor.class);
    }
}
