package cn.jowen.framework.extras.web.sign;

import org.jspecify.annotations.NullMarked;

/**
 * 签名校验器抽象。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface SignVerifier {

    /**
     * 校验签名。
     *
     * @param appId     应用标识（用于选取密钥）
     * @param payload   待签名原始串
     * @param signature 客户端签名
     * @param timestamp 请求时间戳（毫秒）
     * @return {@code true} 表示验签通过
     */
    boolean verify(String appId, String payload, String signature, long timestamp);
}
