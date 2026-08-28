package cn.jowen.framework.demo.controller;

import cn.jowen.framework.extras.web.captcha.Captcha;
import cn.jowen.framework.extras.web.captcha.CaptchaService;
import cn.jowen.framework.extras.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码示例：生成与校验。
 *
 * <p>依赖 {@link CaptchaService}（由框架自动装配；类型由
 * {@code application.yml} 的 {@code framework.extras.captcha.type} 决定，demo 使用 MATH）。</p>
 *
 * <p>调用示例：</p>
 * <pre>
 * curl http://localhost:8080/demo/captcha/gen          # 返回 {key, code?}
 * curl -X POST http://localhost:8080/demo/captcha/check?key=xxx&code=42
 * </pre>
  * @author Jowen
  * @since 0.0.1
 * @version 0.0.1
 */
@RestController
@RequestMapping("/demo/captcha")
public class CaptchaDemoController {

    /** captchaService 不可变字段。 */
    private final CaptchaService captchaService;

    /**
     * 构造实例。
     * @param captchaService 参数 captchaService
     */
    public CaptchaDemoController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

     /**
      * 执行@ get mapping操作。
      * @return 结果
      */
     * @return 结果
    /** 生成验证码；MATH 类型不直接返回答案，需前端展示图片/题目 */
    @GetMapping("/gen")
    public Result<Captcha> generate() {
        Captcha captcha = captchaService.generate();
        return Result.success(captcha);
    }

     /**
      * 执行@ post mapping操作。
      * @param code 参数 code
      * @return 结果
      */
     * @param code 参数 code
     * @return 结果
    /** 校验验证码；返回 true 表示通过（一次性，校验后立即失效） */
    @PostMapping("/check")
    public Result<Boolean> check(@RequestParam String key, @RequestParam String code) {
        boolean ok = captchaService.validate(key, code);
        return Result.success(ok);
    }
}
