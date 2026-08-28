package cn.jowen.framework.extras.web.sign;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link HmacSha256SignVerifier} 测试。
 *
 * <p>覆盖：正确签名放行、payload/timestamp/appId/secret 任一不匹配即拒绝、
 * 未知 appId 拒绝、签名大小写与长度差异、多应用密钥隔离、构造时 Map 拷贝语义。
 */
class HmacSha256SignVerifierTest {

    private static final String APP_ID = "app-1";
    private static final String SECRET = "s3cr3t-key";

    private static HmacSha256SignVerifier verifier() {
        return new HmacSha256SignVerifier(Map.of(APP_ID, SECRET));
    }

    /** 独立实现一遍期望签名，避免与被测实现共享代码路径。 */
    private static String expectedSignature(String payload, long timestamp, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal((payload + timestamp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- 正向 ----------

    @Test
    void verify_acceptsCorrectSignature() {
        long ts = 1_700_000_000_000L;
        String signature = expectedSignature("payload", ts, SECRET);

        assertThat(verifier().verify(APP_ID, "payload", signature, ts)).isTrue();
    }

    @Test
    void verify_isDeterministicForSameInput() {
        long ts = 1_700_000_000_000L;
        String signature = expectedSignature("payload", ts, SECRET);
        HmacSha256SignVerifier verifier = verifier();

        assertThat(verifier.verify(APP_ID, "payload", signature, ts)).isTrue();
        assertThat(verifier.verify(APP_ID, "payload", signature, ts)).isTrue();
    }

    @Test
    void verify_acceptsEmptyPayload() {
        long ts = 42L;
        assertThat(verifier().verify(APP_ID, "", expectedSignature("", ts, SECRET), ts)).isTrue();
    }

    @Test
    void verify_acceptsUnicodePayload() {
        long ts = 42L;
        String payload = "{\"名称\":\"张三\"}";

        assertThat(verifier().verify(APP_ID, payload, expectedSignature(payload, ts, SECRET), ts)).isTrue();
    }

    @Test
    void signature_lengthIsSha256HexLength() {
        assertThat(expectedSignature("x", 1L, SECRET)).hasSize(64);
    }

    // ---------- 反向 ----------

    @Test
    void verify_rejectsTamperedPayload() {
        long ts = 1_700_000_000_000L;
        String signature = expectedSignature("payload", ts, SECRET);

        assertThat(verifier().verify(APP_ID, "payload-modified", signature, ts)).isFalse();
    }

    @Test
    void verify_rejectsTamperedTimestamp() {
        long ts = 1_700_000_000_000L;
        String signature = expectedSignature("payload", ts, SECRET);

        assertThat(verifier().verify(APP_ID, "payload", signature, ts + 1)).isFalse();
    }

    @Test
    void verify_rejectsTamperedSignature() {
        long ts = 1_700_000_000_000L;
        String signature = expectedSignature("payload", ts, SECRET);
        String tampered = ("0".equals(signature.substring(0, 1)) ? "1" : "0") + signature.substring(1);

        assertThat(verifier().verify(APP_ID, "payload", tampered, ts)).isFalse();
    }

    @Test
    void verify_rejectsSignatureWithDifferentLength() {
        long ts = 1L;
        String signature = expectedSignature("payload", ts, SECRET);

        assertThat(verifier().verify(APP_ID, "payload", signature + "0", ts)).isFalse();
        assertThat(verifier().verify(APP_ID, "payload", signature.substring(1), ts)).isFalse();
        assertThat(verifier().verify(APP_ID, "payload", "", ts)).isFalse();
    }

    @Test
    void verify_isCaseSensitive() {
        long ts = 1L;
        String signature = expectedSignature("payload", ts, SECRET);
        String upper = signature.toUpperCase();

        assertThat(upper).as("SHA-256 hex 必含字母，大小写形式应不同").isNotEqualTo(signature);
        assertThat(verifier().verify(APP_ID, "payload", upper, ts))
                .as("实现输出小写 hex，大写签名应被拒绝")
                .isFalse();
    }

    @Test
    void verify_rejectsUnknownAppId() {
        long ts = 1L;
        String signature = expectedSignature("payload", ts, SECRET);

        assertThat(verifier().verify("unknown-app", "payload", signature, ts)).isFalse();
    }

    @Test
    void verify_rejectsWhenSignedWithWrongSecret() {
        long ts = 1L;
        String signature = expectedSignature("payload", ts, "another-secret");

        assertThat(verifier().verify(APP_ID, "payload", signature, ts)).isFalse();
    }

    // ---------- 多应用隔离 ----------

    @Test
    void verify_isolatesSecretsPerApp() {
        long ts = 99L;
        HmacSha256SignVerifier verifier =
                new HmacSha256SignVerifier(Map.of("a", "secret-a", "b", "secret-b"));

        String signedForA = expectedSignature("payload", ts, "secret-a");

        assertThat(verifier.verify("a", "payload", signedForA, ts)).isTrue();
        assertThat(verifier.verify("b", "payload", signedForA, ts)).isFalse();
    }

    /**
     * 现状行为记录：配置了空串密钥时，{@code SecretKeySpec} 会抛
     * {@link IllegalArgumentException}（"Empty key"），而该异常不在实现的 catch 列表内，
     * 因此直接向外抛出而非返回 {@code false}。属于配置健壮性隐患，已在测试报告中标注。
     */
    @Test
    void verify_withEmptySecretConfigured_propagatesIllegalArgument() {
        long ts = 7L;
        HmacSha256SignVerifier verifier = new HmacSha256SignVerifier(Map.of("app", ""));

        assertThatThrownBy(() -> verifier.verify("app", "payload", "0".repeat(64), ts))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyConfiguration_rejectsEverything() {
        assertThat(new HmacSha256SignVerifier(Map.of()).verify("any", "payload", "sig", 1L)).isFalse();
    }

    // ---------- 构造语义 ----------

    @Test
    void constructor_copiesSecretMap_soLaterMutationHasNoEffect() {
        Map<String, String> source = new HashMap<>();
        source.put(APP_ID, SECRET);
        HmacSha256SignVerifier verifier = new HmacSha256SignVerifier(source);

        source.clear();

        long ts = 5L;
        assertThat(verifier.verify(APP_ID, "payload", expectedSignature("payload", ts, SECRET), ts))
                .isTrue();
    }

    @Test
    void implementsSignVerifierContract() {
        assertThat(verifier()).isInstanceOf(SignVerifier.class);
    }
}
