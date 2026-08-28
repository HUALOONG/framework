package cn.jowen.framework.demo.controller;

import cn.jowen.framework.extras.common.Result;
import cn.jowen.framework.extras.web.crypto.Encrypt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.jowen.framework.demo.dto.PaymentRequest;

/**
 * 加解密示例：请求体解密 + 响应体加密。
 *
 * <p>使用 {@link Encrypt} 后：</p>
 * <ul>
 *   <li>入参：框架在 {@code WebRequestBodyAdvice} 中按 {@code keyAlias} 自动解密并绑定到 {@link PaymentRequest}；</li>
 *   <li>出参：框架在 {@code WebResponseBodyAdvice} 中按同一密钥自动加密后再返回。</li>
 * </ul>
 *
 * <p>密钥在 {@code application.yml} 的 {@code framework.extras.crypto} 中配置（demo 使用内嵌 base64 密钥）。</p>
  * @author Jowen
  * @since 0.0.1
 * @version 0.0.1
 */
@RestController
@RequestMapping("/demo/crypto")
public class CryptoDemoController {

    /** 对请求解密、对响应加密（默认使用 alias=default 的密钥） */
    @Encrypt(keyAlias = "default")
    @PostMapping("/pay")
    public Result<PaymentRequest> pay(@RequestBody PaymentRequest req) {
        // req 已是解密后的明文
        return Result.success(req);
    }
}
