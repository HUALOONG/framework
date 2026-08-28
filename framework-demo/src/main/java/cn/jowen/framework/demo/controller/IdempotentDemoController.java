package cn.jowen.framework.demo.controller;

import cn.jowen.framework.extras.common.Result;
import cn.jowen.framework.extras.web.idempotent.Idempotent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.jowen.framework.demo.dto.PaymentRequest;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 幂等示例：防止重复提交。
 *
 * <p>{@link Idempotent#key()} 支持 SpEL，使用请求体中的业务标识作为幂等键；
 * 同一 key 在 {@link Idempotent#expire()} 秒内的重复请求会被拒绝
 * （默认抛 {@code IdempotentException}，由全局异常处理器转换为 409）。</p>
 *
 * <p>调用示例（连续两次相同 bizId 即触发幂等拦截）：</p>
 * <pre>
 * curl -X POST http://localhost:8080/demo/idempotent/pay \
 *   -H 'Content-Type: application/json' \
 *   -d '{"bizId":"ORDER-001","amount":9.9}'
 * </pre>
  * @author Jowen
  * @since 0.0.1
 * @version 0.0.1
 */
@RestController
@RequestMapping("/demo/idempotent")
public class IdempotentDemoController {

    /** counter 不可变字段。 */
    private final AtomicLong counter = new AtomicLong();

    /** PaymentRequest 字段。 */
    @PostMapping("/pay")
    @Idempotent(key = "#req.bizId", expire = 30, message = "订单正在处理中，请勿重复提交")
    public Result<Map<String, Object>> pay(@RequestBody PaymentRequest req) {
        long seq = counter.incrementAndGet();
        return Result.success(Map.of(
                "bizId", req.getBizId(),
                "processedSeq", seq,
                "note", "首次提交成功；30 秒内相同 bizId 的重复提交将被幂等拦截"
        ));
    }
}
