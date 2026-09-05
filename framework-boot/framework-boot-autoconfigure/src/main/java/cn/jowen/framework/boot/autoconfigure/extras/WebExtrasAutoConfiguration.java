package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.properties.DataScope;
import cn.jowen.framework.extras.web.captcha.ArithmeticCaptchaGenerator;
import cn.jowen.framework.extras.web.captcha.CaptchaGenerator;
import cn.jowen.framework.extras.web.captcha.CaptchaService;
import cn.jowen.framework.extras.web.captcha.CaptchaStore;
import cn.jowen.framework.extras.web.captcha.GraphicCaptchaGenerator;
import cn.jowen.framework.extras.web.captcha.RedisCaptchaStore;
import cn.jowen.framework.extras.web.crypto.AesGcmCryptoProcessor;
import cn.jowen.framework.extras.web.crypto.CryptoAdvice;
import cn.jowen.framework.extras.web.crypto.CryptoProcessor;
import cn.jowen.framework.extras.web.captcha.SmsCaptchaGenerator;
import cn.jowen.framework.extras.web.captcha.SmsCaptchaSender;
import cn.jowen.framework.extras.web.datapermission.DataPermissionAspect;
import cn.jowen.framework.extras.web.desensitize.DesensitizeAspect;
import cn.jowen.framework.extras.web.excel.ExcelExporter;
import cn.jowen.framework.extras.web.excel.ExcelImporter;
import cn.jowen.framework.extras.web.datapermission.DataPermissionRule;
import cn.jowen.framework.extras.web.datapermission.DataPermissionUserProvider;
import cn.jowen.framework.extras.web.idempotent.IdempotentAspect;
import cn.jowen.framework.extras.web.idempotent.IdempotentStore;
import cn.jowen.framework.extras.web.idempotent.LocalIdempotentStore;
import cn.jowen.framework.extras.web.idempotent.RedisIdempotentStore;
import cn.jowen.framework.extras.web.lock.DistributedLock;
import cn.jowen.framework.extras.web.lock.LockAspect;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import cn.jowen.framework.extras.web.lock.RedisDistributedLock;
import cn.jowen.framework.extras.web.operatelog.OperateLogAspect;
import cn.jowen.framework.extras.web.operatelog.OperateLogHandler;
import cn.jowen.framework.extras.web.operatelog.OperatorProvider;
import cn.jowen.framework.extras.web.ratelimit.RateLimitAspect;
import cn.jowen.framework.extras.web.ratelimit.RateLimiterManager;
import cn.jowen.framework.extras.web.sign.HmacSha256SignVerifier;
import cn.jowen.framework.extras.web.sign.SignInterceptor;
import cn.jowen.framework.extras.web.sign.SignVerifier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

/**
 * Web 扩展能力自动装配：锁 / 限流 / 幂等 / 加解密 / 签名 / 验证码 / 操作日志 / 数据权限。
 *
 * <p>装配条件（两级总开关，均默认开启）：
 * <ul>
 *   <li>{@code framework.extras.enabled=true}；</li>
 *   <li>{@code framework.extras.web.enabled=true}；</li>
 *   <li>classpath 存在 Spring AOP（AspectJ 注解）。</li>
 * </ul>
 *
 * <p>能力级开关（各能力独立 {@code @ConditionalOnProperty}，互不影响）：
 * <pre>{@code
 * framework.extras.web:
 *   enabled: true
 *   lock.enabled: true
 *   ratelimit.enabled: true
 *   idempotent.enabled: true
 *   crypto.enabled: true
 *   sign.enabled: true
 *   captcha.enabled: false        # 默认关闭
 *   datapermission.enabled: false # 默认关闭
 *   operatelog.enabled: false     # 默认关闭
 * }</pre>
 *
 * <p>配置统一绑定自 {@link BootWebExtrasProperties}（前缀 {@code framework.extras.web}），
 * 由构造注入提供。绑定类置于 Boot 层，web 实现模块保持零 Spring 依赖。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "framework.extras.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({BootWebExtrasProperties.class, BootExtrasProperties.class})
public class WebExtrasAutoConfiguration {

    /** props 不可变字段。 */
    private final BootWebExtrasProperties props;

    /**
     * 构造实例。
     * @param props 参数 props
     */
    public WebExtrasAutoConfiguration(BootWebExtrasProperties props) {
        this.props = props;
    }

    /** RateLimiterManager 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.ratelimit", name = "enabled", matchIfMissing = true)
    public RateLimiterManager rateLimiterManager() {
        return new RateLimiterManager();
    }

    /** RateLimitAspect 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.ratelimit", name = "enabled", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect(RateLimiterManager manager) {
        return new RateLimitAspect(manager);
    }

    /**
     * IdempotentStore 字段。
     *
     * <p>存在 {@code RedisCommandExecutor} 时自动切换为 Redis 实现（多实例共享），
     * 否则回退本地内存实现（单机兜底）。用 {@code ObjectProvider} 惰性解析而非
     * {@code @ConditionalOnBean}，是为了规避自动配置中 bean 注册顺序导致的
     * 条件判断失效问题。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "framework.extras.web.idempotent", name = "enabled", matchIfMissing = true)
    public IdempotentStore idempotentStore(ObjectProvider<RedisCommandExecutor> redisExecutor) {
        RedisCommandExecutor executor = redisExecutor.getIfAvailable();
        return executor != null ? new RedisIdempotentStore(executor) : new LocalIdempotentStore();
    }

    /** IdempotentAspect 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.idempotent", name = "enabled", matchIfMissing = true)
    public IdempotentAspect idempotentAspect(IdempotentStore store) {
        return new IdempotentAspect(store);
    }

    /** DistributedLock 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.lock", name = "enabled", matchIfMissing = true)
    public LockAspect lockAspect(@Nullable DistributedLock distributedLock) {
        return new LockAspect(distributedLock);
    }

    /** CryptoProcessor 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.crypto", name = "enabled", matchIfMissing = true)
    public CryptoProcessor cryptoProcessor() {
        Map<String, byte[]> keys = new HashMap<>();
        for (Map.Entry<String, String> e : props.getCrypto().getKeys().entrySet()) {
            keys.put(e.getKey(), Base64.getDecoder().decode(e.getValue()));
        }
        return new AesGcmCryptoProcessor(props.getCrypto().getDefaultKeyAlias(), keys);
    }

    /** CryptoAdvice 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.crypto", name = "enabled", matchIfMissing = true)
    public CryptoAdvice cryptoAdvice(CryptoProcessor processor) {
        return new CryptoAdvice(processor);
    }

    /** SignVerifier 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.sign", name = "enabled", matchIfMissing = true)
    public SignVerifier signVerifier() {
        return new HmacSha256SignVerifier(props.getSign().getAppSecrets());
    }

    /** SignInterceptor 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.sign", name = "enabled", matchIfMissing = true)
    public SignInterceptor signInterceptor(SignVerifier verifier) {
        return new SignInterceptor(verifier);
    }

    /**
     * CaptchaStore 字段。
     *
     * <p>存在 {@code RedisCommandExecutor} 时自动切换为 Redis 实现，
     * 使「A 节点生成、B 节点校验」的多实例场景直接可用；否则回退内存实现。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "framework.extras.web.captcha", name = "enabled", havingValue = "true")
    public CaptchaStore captchaStore(ObjectProvider<RedisCommandExecutor> redisExecutor) {
        RedisCommandExecutor executor = redisExecutor.getIfAvailable();
        return executor != null ? new RedisCaptchaStore(executor) : new CaptchaStore.InMemory();
    }

    /** CaptchaGenerator 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.captcha", name = "enabled", havingValue = "true")
    public CaptchaGenerator arithmeticCaptchaGenerator() {
        return new ArithmeticCaptchaGenerator(props.getCaptcha().getLength());
    }

    /** CaptchaGenerator 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.captcha", name = "enabled", havingValue = "true")
    public CaptchaGenerator graphicCaptchaGenerator() {
        return new GraphicCaptchaGenerator(
                props.getCaptcha().getWidth(), props.getCaptcha().getHeight(), props.getCaptcha().getLength());
    }

    /** CaptchaGenerator 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.captcha", name = "enabled", havingValue = "true")
    public CaptchaService captchaService(CaptchaStore store, List<CaptchaGenerator> generators) {
        return new CaptchaService(store, generators, props.getCaptcha());
    }

    /** OperateLogHandler 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.operatelog", name = "enabled", havingValue = "true")
    public OperateLogHandler operateLogHandler() {
        return new OperateLogHandler.Slf4j();
    }

    /** OperatorProvider 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.operatelog", name = "enabled", havingValue = "true")
    public OperatorProvider operatorProvider() {
        return OperatorProvider.NONE;
    }

    /** OperatorProvider 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.operatelog", name = "enabled", havingValue = "true")
    public OperateLogAspect operateLogAspect(OperateLogHandler handler, OperatorProvider provider) {
        return new OperateLogAspect(handler, ForkJoinPool.commonPool(),
                props.getOperateLog().isAsync(), provider);
    }

    /** DataPermissionUserProvider 字段。 */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.datapermission", name = "enabled", havingValue = "true")
    public DataPermissionRule dataPermissionRule(
            @Nullable DataPermissionUserProvider userProvider) {
        // 注入了用户上下文提供者时使用 SQL 条件实现；否则回落到 Default（不过滤），
        // 保持与既有行为一致，避免因缺少用户上下文导致启动失败。
        DataPermissionUserProvider provider =
                userProvider == null ? DataPermissionUserProvider.NONE : userProvider;
        if (provider == DataPermissionUserProvider.NONE) {
            return new DataPermissionRule.Default();
        }
        return new DataPermissionRule.SqlDefault(
                props.getDataPermission().getDeptColumn(),
                props.getDataPermission().getUserColumn(),
                provider);
    }

    /**
     * 数据权限切面。
     *
     * <p>方法未标注 {@code @DataPermission} 时使用
     * {@code framework.extras.datapermission.default-scope}（默认 {@link DataScope#ALL}），
     * 使该配置真正生效而非形同虚设；标注了注解的方法始终以注解声明的范围为准。
     *
     * @param rule 数据权限规则
     * @param commonProps 公共扩展配置，提供默认数据范围
     * @return 数据权限切面
     */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.datapermission", name = "enabled", havingValue = "true")
    public DataPermissionAspect dataPermissionAspect(DataPermissionRule rule,
                                                     BootExtrasProperties commonProps) {
        return new DataPermissionAspect(rule, commonProps.getDatapermission().getDefaultScope());
    }

    /**
     * 脱敏切面：方法标注 {@link Desensitized} 后，返回值按 {@code @DesensitizeField} 策略脱敏。
     *
     * <p>默认关闭（{@code framework.extras.web.desensitize.enabled=false}），
     * 需业务方显式开启，避免无差别改写返回值。
     *
     * @return 脱敏切面，不可为 {@code null}
     */
    @Bean
    @ConditionalOnProperty(prefix = "framework.extras.web.desensitize", name = "enabled", havingValue = "true")
    public DesensitizeAspect desensitizeAspect() {
        return new DesensitizeAspect();
    }

    /**
     * Excel 装配：仅当 classpath 存在 EasyExcel 时生效。
     *
     * <p><b>为什么必须独立成内部类</b>：{@code easyexcel} 为 optional 依赖（会传递引入 POI，
     * 体积较大）。隔离在本类中并以 {@code @ConditionalOnClass(name = ...)}（字符串形式）保护，
     * 确保未引入时本类整体被跳过，不影响其他 web 能力。
     */
    @NullMarked
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.alibaba.excel.EasyExcel")
    @ConditionalOnProperty(prefix = "framework.extras.web.excel", name = "enabled", havingValue = "true")
    public static class ExcelConfiguration {

        private final BootWebExtrasProperties props;

        /**
         * 构造实例。
         *
         * @param props Web 扩展配置
         */
        public ExcelConfiguration(BootWebExtrasProperties props) {
            this.props = props;
        }

        /**
         * Excel 导出器。
         *
         * @return 导出器，不可为 {@code null}
         */
        @Bean
        @ConditionalOnMissingBean(ExcelExporter.class)
        public ExcelExporter excelExporter() {
            return new ExcelExporter(props.getExcel().getMaxRows());
        }

        /**
         * Excel 导入器。
         *
         * @return 导入器，不可为 {@code null}
         */
        @Bean
        @ConditionalOnMissingBean(ExcelImporter.class)
        public ExcelImporter excelImporter() {
            return new ExcelImporter(props.getExcel().getMaxRows());
        }
    }

    /**
     * 分布式锁装配：仅当 classpath 存在 Redisson 时生效。
     *
     * <p><b>为什么必须独立成内部类</b>：{@code RedissonClient} 与 {@link RedisDistributedLock}
     * 所需的 Redis 通信能力均为 optional 依赖。将它们隔离在本类中，并以
     * {@code @ConditionalOnClass(name = ...)}（字符串形式）保护，可确保未引入 Redisson 时
     * 本类整体被跳过，不会因类加载失败导致整个 web extras 装配不可用。
     *
     * <p><b>解决的问题</b>：此前 {@code @Lockable(type = REDIS)} 因容器中始终没有
     * {@link cn.jowen.framework.extras.web.lock.DistributedLock} 实现，会静默降级为本地锁
     * （仅打印 warn 日志），导致多实例部署时锁失效而不易察觉。
     * 引入 Redisson 后本类自动注册 {@link RedisDistributedLock}，使 REDIS 锁真正生效。
     *
     * <p>业务方若需使用其他 Redis 客户端（如 Lettuce/Jedis 直连），可自行定义
     * {@code RedisCommandExecutor} 与 {@code DistributedLock} Bean，
     * 本装配因 {@code @ConditionalOnMissingBean} 自动让位。
     */
    @NullMarked
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.redisson.api.RedissonClient")
    @ConditionalOnProperty(prefix = "framework.extras.web.lock", name = "enabled", matchIfMissing = true)
    public static class RedisLockConfiguration {

        /**
         * 基于 Redisson 的分布式锁实现。
         *
         * @param client Redisson 客户端（由业务方引入 starter 后由 Spring Boot 自动配置）
         * @param props  Web 扩展配置，提供默认租约时间
         * @return 分布式锁，不可为 {@code null}
         */
        @Bean
        @ConditionalOnMissingBean(DistributedLock.class)
        @ConditionalOnBean(org.redisson.api.RedissonClient.class)
        public DistributedLock distributedLock(org.redisson.api.RedissonClient client,
                                               BootWebExtrasProperties props) {
            return new RedisDistributedLock(
                    new RedissonCommandExecutor(client),
                    props.getLock().getDefaultLeaseMillis());
        }
    }

    /**
     * 短信验证码装配：仅当引入 {@code framework-extras-message} 且存在 {@code MessageService} 时生效。
     *
     * <p><b>为什么必须独立成内部类</b>：{@code MessageService} 属 optional 依赖。
     * 以 {@code @ConditionalOnClass(name = ...)}（字符串形式）+ {@code @ConditionalOnBean}
     * 双重保护，确保未引入 message 模块时本类整体被跳过，不影响其他验证码类型。
     *
     * <p><b>解决的问题</b>：短信验证码此前无任何默认下发实现，必须业务方自行注入
     * {@code SmsCaptchaSender} 才能使用。引入 message 模块后此处自动装配桥接实现，
     * 短信验证码开箱即用。业务方自定义实现优先（{@code @ConditionalOnMissingBean}）。
     */
    @NullMarked
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "cn.jowen.framework.extras.message.core.MessageService")
    @ConditionalOnBean(cn.jowen.framework.extras.message.core.MessageService.class)
    @ConditionalOnProperty(prefix = "framework.extras.web.captcha", name = "enabled", havingValue = "true")
    public static class SmsCaptchaConfiguration {

        /**
         * 短信验证码发送器：桥接到 message 模块。
         *
         * @param messageService 消息服务
         * @return 短信验证码发送器
         */
        @Bean
        @ConditionalOnMissingBean(SmsCaptchaSender.class)
        public SmsCaptchaSender smsCaptchaSender(
                cn.jowen.framework.extras.message.core.MessageService messageService) {
            return new MessageServiceSmsCaptchaSender(messageService);
        }

        /**
         * 短信验证码生成器：注册后 {@code CaptchaService.generateSms(phone)} 方可使用。
         *
         * @param sender 短信发送器
         * @param props  Web 扩展配置，提供验证码长度
         * @return 短信验证码生成器
         */
        @Bean
        @ConditionalOnMissingBean(SmsCaptchaGenerator.class)
        public SmsCaptchaGenerator smsCaptchaGenerator(SmsCaptchaSender sender,
                                                       BootWebExtrasProperties props) {
            return new SmsCaptchaGenerator(props.getCaptcha().getLength(), sender);
        }
    }
}
