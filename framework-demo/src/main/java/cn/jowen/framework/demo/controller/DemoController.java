package cn.jowen.framework.demo.controller;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.extras.common.Result;
import cn.jowen.framework.extras.web.ratelimit.RateLimit;
import cn.jowen.framework.i18n.api.MessageSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 框架能力演示接口：国际化 / 缓存统计 / 限流 / 装配自检。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /** i18n 资源 basename 为 messages，对应 resources 下的 messages*.properties */
    private final MessageSource messageSource;
    /** cacheManager 不可变字段。 */
    private final CacheManager cacheManager;

    /**
     * 构造实例。
     * @param messageSource 参数 messageSource
     * @param cacheManager 参数 cacheManager
     */
    public DemoController(@Qualifier("frameworkMessageSource") MessageSource messageSource,
                          CacheManager cacheManager) {
        this.messageSource = messageSource;
        this.cacheManager = cacheManager;
    }

    /**
     * 国际化演示：按 {@code locale} 参数返回不同语言的文案。
     *
     * <p>示例：
     * <ul>
     *   <li>{@code /api/demo/hello?name=Alice} —— 默认 zh_CN</li>
     *   <li>{@code /api/demo/hello?name=Alice&locale=en_US} —— 英文</li>
     * </ul>
     *
     * @param name   用户名
     * @param locale 区域，形如 {@code zh_CN} / {@code en_US}
     * @return 本地化后的问候语
     */
    @GetMapping("/hello")
    public Result<Map<String, Object>> hello(@RequestParam(defaultValue = "World") String name,
                                             @RequestParam(defaultValue = "zh_CN") String locale) {
        Locale target = parseLocale(locale);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("welcome", messageSource.getMessage("demo.welcome", target, null));
        data.put("greeting", messageSource.getMessage("demo.greeting", target, new Object[]{name, target}));
        data.put("locale", target.toString());
        return Result.success(data);
    }

    /**
     * 缓存统计：展示框架自动采集的缓存名称与命中率。
     *
     * @return 各缓存的统计信息
     */
    @GetMapping("/cache-stats")
    public Result<Map<String, Object>> cacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        for (String name : cacheManager.cacheNames()) {
            var snapshot = cacheManager.getStats(name);
            Map<String, Object> item = new LinkedHashMap<>();
            if (snapshot != null) {
                item.put("hits", snapshot.hits());
                item.put("misses", snapshot.misses());
                item.put("total", snapshot.total());
                item.put("hitRate", snapshot.hitRate());
            }
            stats.put(name, item);
        }
        return Result.success(stats);
    }

    /**
     * 限流演示：同一 key 每分钟 5 次，超出后返回限流提示。
     *
     * @return 当前调用次数说明
     */
    @GetMapping("/ratelimit")
    @RateLimit(key = "'demo:ratelimit'", permits = 5, window = 1,
            message = "演示接口触发限流，请稍后再试")
    public Result<String> ratelimit() {
        return Result.success("本次请求未触发限流");
    }

    /**
     * 装配自检：返回当前生效的框架能力，便于确认按需装配是否成功。
     *
     * @return 能力清单
     */
    @GetMapping("/capabilities")
    public Result<Map<String, Object>> capabilities() {
        Map<String, Object> map = new HashMap<>();
        map.put("data", "framework-data-jdbc（H2 内存库）");
        map.put("cache", "framework-cache（Caffeine 本地缓存）");
        map.put("i18n", "framework-i18n（properties 源）");
        map.put("logger", "framework-logger（日志脱敏已开启）");
        map.put("boot-web", "限流 / 幂等 / 加解密 / 签名 / 验证码 / 操作日志");
        map.put("extras-storage", "本地文件存储");
        map.put("extras-message", "消息门面（未注入发送器时不实际投递）");
        return Result.success(map);
    }

    /** 将 {@code zh_CN} / {@code en-US} 形式解析为 Locale */
    private static Locale parseLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.forLanguageTag(locale.replace('_', '-'));
    }
}
