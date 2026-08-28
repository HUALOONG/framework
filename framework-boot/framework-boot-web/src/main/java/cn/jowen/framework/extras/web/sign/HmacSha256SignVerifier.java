package cn.jowen.framework.extras.web.sign;

import org.jspecify.annotations.NullMarked;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 HMAC-SHA256 的 {@link SignVerifier} 实现，按 appId 管理密钥。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class HmacSha256SignVerifier implements SignVerifier {

    private final Map<String, String> appSecrets = new ConcurrentHashMap<>();

    public HmacSha256SignVerifier(Map<String, String> appSecrets) {
        this.appSecrets.putAll(appSecrets);
    }

    @Override
    public boolean verify(String appId, String payload, String signature, long timestamp) {
        String secret = appSecrets.get(appId);
        if (secret == null) {
            return false;
        }
        String expected = sign(payload + timestamp, secret);
        return constantTimeEquals(expected, signature);
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("签名计算失败", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
