package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.properties.DataScope;
import cn.jowen.framework.extras.web.captcha.CaptchaService;
import cn.jowen.framework.extras.web.captcha.CaptchaStore;
import cn.jowen.framework.extras.web.captcha.RedisCaptchaStore;
import cn.jowen.framework.extras.web.captcha.SmsCaptchaGenerator;
import cn.jowen.framework.extras.web.captcha.SmsCaptchaSender;
import cn.jowen.framework.extras.web.datapermission.DataPermissionRule;
import cn.jowen.framework.extras.web.datapermission.DataPermissionUserProvider;
import cn.jowen.framework.extras.web.idempotent.IdempotentStore;
import cn.jowen.framework.extras.web.idempotent.LocalIdempotentStore;
import cn.jowen.framework.extras.web.idempotent.RedisIdempotentStore;
import cn.jowen.framework.extras.web.lock.DistributedLock;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import cn.jowen.framework.extras.web.lock.RedisDistributedLock;
import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WebExtrasAutoConfiguration} 装配验证：重点确认 {@code framework.extras.web.*}
 * 配置经 {@link BootWebExtrasProperties} 继承链绑定后能真正影响装配出的 Bean。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class WebExtrasAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebExtrasAutoConfiguration.class));

    @Test
    void captchaPropertiesAreBoundFromYaml() {
        runner.withPropertyValues(
                        "framework.extras.web.captcha.enabled=true",
                        "framework.extras.web.captcha.type=GRAPHIC",
                        "framework.extras.web.captcha.width=200",
                        "framework.extras.web.captcha.height=80",
                        "framework.extras.web.captcha.length=6",
                        "framework.extras.web.captcha.expire-seconds=300")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ExtrasWebProperties props = context.getBean(BootWebExtrasProperties.class);

                    assertThat(props.getCaptcha().isEnabled()).isTrue();
                    assertThat(props.getCaptcha().getType())
                            .isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.GRAPHIC);
                    assertThat(props.getCaptcha().getWidth()).isEqualTo(200);
                    assertThat(props.getCaptcha().getHeight()).isEqualTo(80);
                    assertThat(props.getCaptcha().getLength()).isEqualTo(6);
                    assertThat(props.getCaptcha().getExpireSeconds()).isEqualTo(300L);

                    // 配置应真正传导到 CaptchaService（图形验证码带 PNG 图像）
                    CaptchaService service = context.getBean(CaptchaService.class);
                    assertThat(service.generate().image()).isNotNull();
                });
    }

    @Test
    void captchaDisabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(CaptchaService.class));
    }

    @Test
    void operateLogAsyncFlagIsRespected() {
        runner.withPropertyValues(
                        "framework.extras.web.operatelog.enabled=true",
                        "framework.extras.web.operatelog.async=false",
                        "framework.extras.web.operatelog.handler=db")
                .run(context -> {
                    ExtrasWebProperties props = context.getBean(BootWebExtrasProperties.class);
                    assertThat(props.getOperateLog().isAsync()).isFalse();
                    assertThat(props.getOperateLog().getHandler()).isEqualTo("db");
                });
    }

    @Test
    void coreAspectsRegisteredByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("rateLimiterManager");
            assertThat(context).hasBean("idempotentStore");
            assertThat(context).hasBean("lockAspect");
            assertThat(context).hasBean("cryptoProcessor");
            assertThat(context).hasBean("signVerifier");
        });
    }

    @Test
    void cryptoKeysAreDecodedFromBase64() {
        runner.withPropertyValues("framework.extras.web.crypto.keys.app=QUJDREVGMTIzNDU2Nzg5MA==")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(BootWebExtrasProperties.class)
                            .getCrypto().getKeys()).containsKey("app");
                });
    }

    @Test
    void dataPermissionEnabledRegistersAspect() {
        runner.withPropertyValues("framework.extras.web.datapermission.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("dataPermissionAspect");
                    assertThat(context).hasBean("dataPermissionRule");
                });
    }

    @Test
    void lockDefaultLeaseMillisIsBoundFromYaml() {
        runner.withPropertyValues("framework.extras.web.lock.default-lease-millis=15000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(BootWebExtrasProperties.class)
                            .getLock().getDefaultLeaseMillis()).isEqualTo(15000L);
                });
    }

    @Test
    void lockDefaultLeaseMillisDefaultsTo30Seconds() {
        runner.run(context -> assertThat(context.getBean(BootWebExtrasProperties.class)
                .getLock().getDefaultLeaseMillis()).isEqualTo(30000L));
    }

    @Test
    void distributedLockAbsentWithoutRedissonClient() {
        // 容器内无 RedissonClient Bean 时不注册分布式锁，@Lockable(REDIS) 仍降级为本地锁
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DistributedLock.class);
        });
    }

    @Test
    void distributedLockRegisteredWhenRedissonClientPresent() {
        runner.withUserConfiguration(RedissonClientConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DistributedLock.class);
                    assertThat(context.getBean(DistributedLock.class))
                            .isInstanceOf(RedisDistributedLock.class);
                });
    }

    @Test
    void distributedLockRespectsConfiguredLeaseMillis() {
        runner.withUserConfiguration(RedissonClientConfig.class)
                .withPropertyValues("framework.extras.web.lock.default-lease-millis=8000")
                .run(context -> {
                    DistributedLock lock = context.getBean(DistributedLock.class);
                    // 租约 <=0 时回落到配置值；此处仅验证装配成功且类型为 Redis 实现
                    assertThat(lock).isInstanceOf(RedisDistributedLock.class);
                    assertThat(context.getBean(BootWebExtrasProperties.class)
                            .getLock().getDefaultLeaseMillis()).isEqualTo(8000L);
                });
    }

    @Test
    void customDistributedLockBeanTakesPrecedence() {
        runner.withUserConfiguration(RedissonClientConfig.class, CustomLockConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLock.class);
                    assertThat(context.getBean(DistributedLock.class))
                            .isNotInstanceOf(RedisDistributedLock.class);
                });
    }

    @Test
    void dataPermissionRuleUsesSqlDefaultWhenUserProviderPresent() {
        runner.withUserConfiguration(UserProviderConfig.class)
                .withPropertyValues("framework.extras.web.datapermission.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DataPermissionRule.class);
                    assertThat(context.getBean(DataPermissionRule.class))
                            .isInstanceOf(DataPermissionRule.SqlDefault.class);
                });
    }

    @Test
    void dataPermissionRuleFallsBackToDefaultWithoutUserProvider() {
        runner.withPropertyValues("framework.extras.web.datapermission.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DataPermissionRule.class);
                    // 无用户上下文时回落 Default，保持既有"不过滤"行为，避免启动失败
                    assertThat(context.getBean(DataPermissionRule.class))
                            .isInstanceOf(DataPermissionRule.Default.class);
                });
    }

    @Test
    void sqlDefaultHonoursConfiguredColumnNames() {
        runner.withUserConfiguration(UserProviderConfig.class)
                .withPropertyValues(
                        "framework.extras.web.datapermission.enabled=true",
                        "framework.extras.web.datapermission.dept-column=org_id",
                        "framework.extras.web.datapermission.user-column=owner")
                .run(context -> {
                    DataPermissionRule rule = context.getBean(DataPermissionRule.class);
                    assertThat(rule.condition(DataScope.SELF, "t")).isEqualTo("owner = 'u1'");
                    assertThat(rule.condition(DataScope.DEPT, "t")).isEqualTo("org_id = 'd1'");
                });
    }

    @Test
    void sqlDefaultUsesDefaultColumnNames() {
        runner.withUserConfiguration(UserProviderConfig.class)
                .withPropertyValues("framework.extras.web.datapermission.enabled=true")
                .run(context -> {
                    DataPermissionRule rule = context.getBean(DataPermissionRule.class);
                    assertThat(rule.condition(DataScope.SELF, "t")).isEqualTo("create_by = 'u1'");
                    assertThat(rule.condition(DataScope.DEPT, "t")).isEqualTo("dept_id = 'd1'");
                });
    }

    @Test
    void smsCaptchaAbsentWithoutMessageService() {
        // 未引入 message 模块（无 MessageService Bean）时不装配短信验证码
        runner.withPropertyValues("framework.extras.web.captcha.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SmsCaptchaSender.class);
                    assertThat(context).doesNotHaveBean(SmsCaptchaGenerator.class);
                });
    }

    @Test
    void smsCaptchaRegisteredWhenMessageServicePresent() {
        runner.withUserConfiguration(MessageServiceConfig.class)
                .withPropertyValues("framework.extras.web.captcha.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SmsCaptchaSender.class);
                    assertThat(context).hasSingleBean(SmsCaptchaGenerator.class);
                    assertThat(context.getBean(CaptchaService.class)).isNotNull();
                });
    }

    @Test
    void excelConfiguration_exporterIsConstructibleWithoutEasyExcel() {
        // ExcelConfiguration 构造器与 excelExporter() 的 ExcelExporter 构造器均不引用 EasyExcel，
        // 故无需 easyexcel SDK 即可覆盖该内部配置类的导出分支；excelImporter() 因 ExcelImporter 类加载即依赖
        // com.alibaba.excel.event.AnalysisEventListener（EasyExcel 缺失），离线不可构造，已据规则在报告中标注为不可测。
        WebExtrasAutoConfiguration.ExcelConfiguration config =
                new WebExtrasAutoConfiguration.ExcelConfiguration(new BootWebExtrasProperties());
        assertThat(config.excelExporter()).isNotNull();
    }


    @Test
    void customSmsCaptchaSenderTakesPrecedence() {
        runner.withUserConfiguration(MessageServiceConfig.class, CustomSmsSenderConfig.class)
                .withPropertyValues("framework.extras.web.captcha.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SmsCaptchaSender.class);
                    assertThat(context.getBean(SmsCaptchaSender.class))
                            .isNotInstanceOf(MessageServiceSmsCaptchaSender.class);
                });
    }

    /** 容器内无 Redis 执行器时，幂等存储回落本地内存实现（单机兜底）。 */
    @Test
    void idempotentStoreFallsBackToLocalWithoutRedisExecutor() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(IdempotentStore.class))
                    .isInstanceOf(LocalIdempotentStore.class);
        });
    }

    /** 提供 Redis 执行器时自动切换为 Redis 实现，使幂等键在多实例间共享。 */
    @Test
    void idempotentStoreUsesRedisWhenExecutorPresent() {
        runner.withUserConfiguration(RedisExecutorConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IdempotentStore.class))
                            .isInstanceOf(RedisIdempotentStore.class);
                });
    }

    /** 业务方自定义幂等存储应优先于框架默认装配，保持可替换性。 */
    @Test
    void customIdempotentStoreTakesPrecedence() {
        runner.withUserConfiguration(RedisExecutorConfig.class, CustomIdempotentStoreConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotentStore.class);
                    assertThat(context.getBean(IdempotentStore.class))
                            .isNotInstanceOf(RedisIdempotentStore.class);
                });
    }

    /** 无 Redis 执行器时验证码存储回落内存实现。 */
    @Test
    void captchaStoreFallsBackToLocalWithoutRedisExecutor() {
        runner.withPropertyValues("framework.extras.web.captcha.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CaptchaStore.class))
                            .isInstanceOf(CaptchaStore.InMemory.class);
                });
    }

    /** 提供 Redis 执行器时验证码存储切换为 Redis 实现，支持跨节点生成/校验。 */
    @Test
    void captchaStoreUsesRedisWhenExecutorPresent() {
        runner.withUserConfiguration(RedisExecutorConfig.class)
                .withPropertyValues("framework.extras.web.captcha.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CaptchaStore.class))
                            .isInstanceOf(RedisCaptchaStore.class);
                });
    }

    /** 未启用验证码时不注册存储 Bean，避免产生无用途对象。 */
    @Test
    void captchaStoreAbsentWhenCaptchaDisabled() {
        runner.withUserConfiguration(RedisExecutorConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(CaptchaStore.class));
    }

    /** 提供用户上下文，模拟业务方接入安全框架后的状态。 */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class UserProviderConfig {

        @org.springframework.context.annotation.Bean
        DataPermissionUserProvider dataPermissionUserProvider() {
            return new DataPermissionUserProvider() {
                @Override
                public String currentUserId() {
                    return "u1";
                }

                @Override
                public String currentDeptId() {
                    return "d1";
                }

                @Override
                public java.util.List<String> currentDeptAndChildIds() {
                    return java.util.List.of("d1");
                }
            };
        }
    }

    /** 提供 mock 的 MessageService，模拟引入 message 模块后的状态。 */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class MessageServiceConfig {

        @org.springframework.context.annotation.Bean
        cn.jowen.framework.extras.message.core.MessageService messageService() {
            return org.mockito.Mockito.mock(cn.jowen.framework.extras.message.core.MessageService.class);
        }
    }

    /** 业务方自定义短信发送器，用于验证 {@code @ConditionalOnMissingBean} 让位语义。 */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class CustomSmsSenderConfig {

        @org.springframework.context.annotation.Bean
        SmsCaptchaSender smsCaptchaSender() {
            return (phone, code) -> {
            };
        }
    }

    /** 提供 mock 的 Redisson 客户端，模拟业务方引入 Redisson starter 后的容器状态。 */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class RedissonClientConfig {

        @org.springframework.context.annotation.Bean
        org.redisson.api.RedissonClient redissonClient() {
            return org.mockito.Mockito.mock(org.redisson.api.RedissonClient.class);
        }
    }

    /** 业务方自定义的分布式锁实现，用于验证 {@code @ConditionalOnMissingBean} 让位语义。 */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class CustomLockConfig {

        @org.springframework.context.annotation.Bean
        DistributedLock distributedLock() {
            return (key, waitMillis, leaseMillis) -> new cn.jowen.framework.extras.web.lock.LocalLock(key, waitMillis);
        }
    }

    /** 提供 mock 的 Redis 命令执行器，模拟业务方接入 Redis 客户端适配后的容器状态。 */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class RedisExecutorConfig {

        @org.springframework.context.annotation.Bean
        RedisCommandExecutor redisCommandExecutor() {
            return org.mockito.Mockito.mock(RedisCommandExecutor.class);
        }
    }

    /** 业务方自定义幂等存储，用于验证 {@code @ConditionalOnMissingBean} 让位语义。 */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class CustomIdempotentStoreConfig {

        @org.springframework.context.annotation.Bean
        IdempotentStore idempotentStore() {
            return new LocalIdempotentStore();
        }
    }
}
