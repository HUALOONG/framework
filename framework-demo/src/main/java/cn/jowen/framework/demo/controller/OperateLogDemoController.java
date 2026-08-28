package cn.jowen.framework.demo.controller;

import cn.jowen.framework.extras.common.Result;
import cn.jowen.framework.extras.web.operatelog.OperateLog;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志示例。
 *
 * <p>使用 {@link OperateLog} 标注后，方法执行（成功/失败）会被 {@code OperateLogAspect}
 * 拦截并通过 {@code OperateLogHandler} 输出（demo 的 {@code LoggingOperateLogHandler}
 * 默认打印到日志；操作人取自 {@code ExtrasFeatureConfig} 中注入的 {@code OperatorProvider}）。</p>
  * @author Jowen
  * @since 0.0.1
 * @version 0.0.1
 */
@RestController
@RequestMapping("/demo/operatelog")
public class OperateLogDemoController {

    /** String 字段。 */
    @OperateLog(module = "demo", value = "更新演示配置")
    @PostMapping("/update")
    public Result<String> update(@RequestParam String configKey) {
        return Result.success("updated: " + configKey);
    }
}
