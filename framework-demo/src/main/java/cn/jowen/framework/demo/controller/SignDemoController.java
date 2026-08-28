package cn.jowen.framework.demo.controller;

import cn.jowen.framework.extras.common.Result;
import cn.jowen.framework.extras.web.sign.Sign;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.jowen.framework.demo.dto.PaymentRequest;

/**
 * 签名示例：接口防篡改校验。
 *
 * <p>{@link Sign} 拦截器会按声明字段顺序 + 时间戳 + 应用密钥计算 HMAC-SHA256，
 * 并与请求头 {@code X-Signature} 比对；同时校验 {@code X-Timestamp} 在允许的时间窗口内。</p>
 *
 * <p>调用示例（需客户端预先用 appSecret 计算签名）：</p>
 * <pre>
 * curl -X POST http://localhost:8080/demo/sign/pay \
 *   -H 'Content-Type: application/json' \
 *   -H 'X-Timestamp: 1700000000000' \
 *   -H 'X-Signature: &lt;hmacsha256(bizId=ORDER-001&amount=9.9&X-Timestamp=1700000000000)&gt;' \
 *   -d '{"bizId":"ORDER-001","amount":9.9}'
 * </pre>
 *
 * <p>appSecret 在 {@code application.yml} 的 {@code framework.extras.sign.app-secret} 配置；
 * 校验失败返回 401。</p>
  * @author Jowen
  * @since 0.0.1
 * @version 0.0.1
 */
@RestController
@RequestMapping("/demo/sign")
public class SignDemoController {

    /** PaymentRequest 字段。 */
    @Sign(fields = {"bizId", "amount"}, header = "X-Signature", timestampHeader = "X-Timestamp")
    @PostMapping("/pay")
    public Result<PaymentRequest> pay(@RequestBody PaymentRequest req) {
        // 能进入方法说明签名校验已通过
        return Result.success(req);
    }
}
