package cn.jowen.framework.demo.config;

import cn.jowen.framework.extras.common.Result;
import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：将框架抛出的业务异常转换为统一的 {@link Result} 响应。
 *
 * <p><b>为什么需要它</b>：框架的切面（限流 / 幂等 / 加解密 / 签名等）在校验不通过时
 * 会抛出 {@link ExtrasException}。框架<b>不预设</b>异常到 HTTP 状态码的映射策略，
 * 这部分由应用自行决定——本类给出一种推荐实现：
 * <ul>
 *   <li>{@code ExtrasException} → 400，附带业务提示文案；</li>
 *   <li>其余异常 → 500，仅返回通用错误，避免泄漏内部细节。</li>
 * </ul>
 *
 * <p><b>扩展提示</b>：若需区分限流（429）、参数校验（400）、鉴权（401）等，
 * 可按异常类型或错误码扩展本类的 {@code @ExceptionHandler} 方法。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** log 常量。 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

     /**
      * 执行@ exception handler操作。
      * @return 结果
      */
     * @return 结果
    /** 框架扩展能力抛出的业务异常 */
    @ExceptionHandler(ExtrasException.class)
    public ResponseEntity<Result<Void>> handleExtras(ExtrasException e) {
        log.warn("业务异常被拦截：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(400, e.getMessage()));
    }

     /**
      * 执行@ exception handler操作。
      * @return 结果
      */
     * @return 结果
    /** 兜底：未预期异常不向客户端暴露堆栈细节 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        log.error("未预期的服务异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "服务内部错误，请联系管理员"));
    }
}
