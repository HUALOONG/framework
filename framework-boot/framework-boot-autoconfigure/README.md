# framework-boot-autoconfigure 模块架构设计

> 文档元信息
> - **模块**：framework-boot-autoconfigure
> - **关键词**：@AutoConfiguration、@Conditional、装配编排、AutoConfiguration.imports、GraalVM AOT
> - **描述**：框架唯一的 Spring Boot 依赖汇聚点，通过自动装配类把各模块能力按需桥接到 Spring 容器
> - **基线**：Spring Boot 4.x + Java 21（Boot 壳依赖收敛单点、@AutoConfiguration 机制）

---

## 一、模块定位

`framework-boot-autoconfigure` 是框架的 **自动装配逻辑层（L3）**，通过 `@AutoConfiguration` + `@Conditional*`
把各模块（logger/data/cache/i18n/plugin/extras）的能力按需装配进 Spring 容器，是 **Spring Boot 依赖的唯一汇聚点**。

**核心价值**：

| 场景            | 没有本模块                                           | 有本模块                                            |
|:----------------|:-----------------------------------------------------|:----------------------------------------------------|
| 模块接入 Spring | 每个模块各自引 spring-boot-autoconfigure、各自写装配 | 全部收敛到本模块单点装配                            |
| 按需启用        | 手动 @Import 或全量装配                              | @ConditionalOn* 声明式按需                          |
| 装配顺序        | 手工维护顺序                                         | @AutoConfigureAfter/Before 显式声明                 |
| 用户扩展        | 无定制口                                             | *Customizer 回调 + @ConditionalOnMissingBean 可覆写 |

**关键架构决策（Boot 依赖收敛）**：

| 决策                          | 内容                                                                                                                                         |
|:------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------|
| ① Boot 壳依赖单点化           | `spring-boot-autoconfigure` 等 Boot 依赖**只进本模块**，其他模块均不直接引用                                                                 |
| ② Spring Framework 无法全排除 | `spring-context`（plugin 子容器）、`spring-expression`（cache/extras SpEL）、`mybatis-spring` 仍需按需引入，但仅作 optional/compile 局部依赖 |
| ③ AutoConfiguration 类上移    | 原各实现模块内的 AutoConfiguration 类统一上移到本模块，实现模块保持纯能力                                                                    |
| ④ imports 注册                | 本模块维护 `META-INF/spring/...AutoConfiguration.imports` 全量注册                                                                           |

**与核心模块的边界**：

| 模块                             | 定位           | 特点                                 |
|:---------------------------------|:---------------|:-------------------------------------|
| **framework-boot-autoconfigure** | **装配编排层** | **唯一的 Boot 依赖汇聚点、按需装配** |
| framework-*-starter              | 聚合入口       | 仅 POM，无代码                       |
| 各实现模块                       | 纯能力         | 零 Spring 依赖（实现层）或 minimal   |

---

## 二、功能清单与依赖矩阵

| 功能        | 子包          | 依赖                             | 说明                                                    |
|:------------|:--------------|:---------------------------------|:--------------------------------------------------------|
| 总装配入口  | 根包          | 全部模块                         | FrameworkAutoConfiguration + Registrar                  |
| Logger 装配 | logger        | framework-logger                 | 日志/脱敏/追踪/MDC                                      |
| 数据装配    | data          | framework-data-core/jdbc/mybatis | DataSource / JDBC / MyBatis                             |
| Cache 装配  | cache         | framework-cache                  | 多级缓存 CacheManager                                   |
| i18n 装配   | i18n          | framework-i18n                   | MessageSource / LocaleResolver                          |
| Plugin 装配 | plugin        | framework-plugin                 | 插件系统桥接                                            |
| Extras 装配 | extras        | framework-extras                 | 工具集 12 功能                                          |
| 健康检查    | health        | 各模块                           | Actuator 聚合                                           |
| 可观测性    | observability | 各模块                           | Micrometer 2.0 聚合                                     |
| Web 适配    | web           | 各模块                           | 拦截器注册                                              |
| 事件桥接    | bridge        | framework-core                   | core.event ↔ Spring ApplicationEvent 双向桥接（可开关） |

---

## 三、整体包结构

```text
framework-boot-autoconfigure
└─ src/main/java/com/framework/boot/autoconfigure/
   ├─ FrameworkAutoConfiguration.java          # 总入口（@Import 各功能装配类）
   ├─ FrameworkAutoConfigurationRegistrar.java # 编程式注册（条件注册辅助）
   │
   ├─ logger/        # FrameworkLoggerAutoConfiguration
   ├─ data/          # DataSourceAutoConfiguration / DataJdbcAutoConfiguration / DataMybatisAutoConfiguration
   ├─ cache/         # FrameworkCacheAutoConfiguration
   ├─ i18n/          # FrameworkI18nAutoConfiguration
   ├─ plugin/        # FrameworkPluginAutoConfiguration
   ├─ extras/        # FrameworkExtrasAutoConfiguration（引用 extras.config.*Factory 逐功能装配）
   ├─ health/        # FrameworkHealthAutoConfiguration（HealthIndicator 聚合）
   ├─ observability/ # FrameworkObservabilityAutoConfiguration（MeterBinder 聚合）
   ├─ web/           # FrameworkWebAutoConfiguration（拦截器/过滤器注册）
   └─ bridge/        # FrameworkEventBridgeAutoConfiguration（core.event ↔ Spring 事件双向桥接）

# 资源
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 四、各子包详细设计

#### 4.1 根包 — 总装配入口

##### 定位

框架装配的单一入口，默认启用（`framework.enabled` 总开关）。

```textmate
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework", name = "enabled", matchIfMissing = true)
@Import({
    FrameworkLoggerAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataJdbcAutoConfiguration.class,
    DataMybatisAutoConfiguration.class,
    FrameworkCacheAutoConfiguration.class,
    FrameworkI18nAutoConfiguration.class,
    FrameworkPluginAutoConfiguration.class,
    FrameworkExtrasAutoConfiguration.class,
    FrameworkHealthAutoConfiguration.class,
    FrameworkObservabilityAutoConfiguration.class,
    FrameworkWebAutoConfiguration.class,
    FrameworkEventBridgeAutoConfiguration.class
})
public class FrameworkAutoConfiguration {
    // 框架级公共 Bean（如 FrameworkMarker、公共 MeterBinder）
}
```

**Registrar 的职责**：`FrameworkAutoConfigurationRegistrar` 处理无法用注解表达的编程式注册（如扫描用户配置的 Customizer
Bean、动态注册模块装配类），在 `registerBeanDefinitions` 中按配置决定注册哪些装配。

#### 4.2 logger/ — Logger 装配

##### 定位

日志能力接入。

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "cn.jowen.framework.logger.facade.LoggerFactory")
@ConditionalOnProperty(prefix = "framework.logger", name = "enabled", matchIfMissing = true)
public class FrameworkLoggerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LoggerBootstrap loggerBootstrap() { ...}          // 初始化 facade → adapter

    @Bean
    @ConditionalOnMissingBean
    public MdcContextPropagation mdcContextPropagation() { ...} // 虚拟线程 MDC 透传

    @Bean
    @ConditionalOnMissingBean
    public SensitiveDataMasker sensitiveDataMasker(SensitiveMaskProperties props) { ...}

    @Bean
    @ConditionalOnMissingBean
    public LogbackStructuredEncoder structuredEncoder() { ...}  // 结构化日志
}
```

#### 4.3 data/ — 数据装配

##### 定位

DataSource + JDBC + MyBatis 三件套，按 `framework.data.type` 二选一。

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "com.zaxxer.hikari.HikariDataSource")
@ConditionalOnProperty(prefix = "framework.data.datasource", name = "enabled", matchIfMissing = true)
public class DataSourceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties props) { ...}   // Hikari，虚拟线程调优参数

    @Bean
    @ConditionalOnMissingBean
    public DataSourceRouter dataSourceRouter() { ...}                 // 多数据源路由（读写分离）
}

@AutoConfiguration
@ConditionalOnClass(name = "cn.jowen.framework.data.jdbc.core.JdbcTemplate")
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "framework.data", name = "type", havingValue = "jdbc")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class DataJdbcAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) { ...}

    @Bean
    @ConditionalOnMissingBean
    public JdbcTransactionManager transactionManager(DataSource dataSource) { ...}
}

@AutoConfiguration
@ConditionalOnClass(name = "com.mybatisflex.core.BaseMapper")
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "framework.data", name = "type", havingValue = "mybatis")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class DataMybatisAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FlexRepositoryFactory flexRepositoryFactory() { ...}       // 仓储工厂

    @Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) { ...} // MyBatis Flex 会话工厂
}
```

**装配顺序**：`DataSource → JDBC/MyBatis`，通过 `@AutoConfigureAfter` 显式声明；用户提供自定义 Bean 时以
`@ConditionalOnMissingBean` 让位。

#### 4.4 cache/ — Cache 装配

##### 定位

缓存能力接入，按 `framework.cache.type` 装配。

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "cn.jowen.framework.cache.api.CacheManager")
@ConditionalOnProperty(prefix = "framework.cache", name = "enabled", matchIfMissing = true)
public class FrameworkCacheAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(CacheProperties props) { ...}     // local/redis/multi 按 type

    @Bean
    @ConditionalOnMissingBean
    public CacheInterceptor cacheInterceptor(CacheManager cacheManager) { ...} // 注解 AOP

    @Bean
    @ConditionalOnMissingBean
    public CacheStatsReporter cacheStatsReporter() { ...}              // Micrometer 2.0

    @Bean
    @ConditionalOnMissingBean
    public CacheHealthIndicator cacheHealthIndicator() { ...}          // Actuator
}
```

#### 4.5 i18n/ — i18n 装配

##### 定位

国际化能力接入。

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "cn.jowen.framework.i18n.api.MessageSource")
@ConditionalOnProperty(prefix = "framework.i18n", name = "enabled", matchIfMissing = true)
public class FrameworkI18nAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MessageSource messageSource(I18nProperties props) { ...}     // 按 SourceType 装配

    @Bean
    @ConditionalOnMissingBean
    public LocaleResolver localeResolver(I18nProperties props) { ...}   // 按 ResolverType 装配

    @Bean
    @ConditionalOnMissingBean
    public I18nInterceptor i18nInterceptor() { ...}                     // Web 拦截器

    @Bean
    @ConditionalOnMissingBean
    public I18nResourceReloader resourceReloader() { ...}               // 热加载
}
```

#### 4.6 plugin/ — Plugin 装配

##### 定位

插件系统桥接（决议 #2 后 plugin.config 无 AutoConfiguration，本类直接装配插件核心组件与 Actuator 端点）。

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "cn.jowen.framework.plugin.api.PluginManager")
@ConditionalOnProperty(prefix = "framework.plugin", name = "enabled", matchIfMissing = false)
public class FrameworkPluginAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public PluginManager pluginManager(PluginProperties props) { ...}

    @Bean
    @ConditionalOnMissingBean
    public PluginCommandLineRunner pluginCommandLineRunner() { ...}     // ApplicationReady 后加载

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    public PluginEndpoint pluginEndpoint(PluginManager manager) { ...}  // Actuator 端点物理位于本模块
}
```

#### 4.7 extras/ — Extras 装配

##### 定位

工具集桥接（决议 #2 后 extras.config 只留 Properties 与工厂，本类引用 `extras.config` 工厂逐功能装配，保持单注册点）。

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "cn.jowen.framework.extras.config.ExtrasProperties")
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", matchIfMissing = true)
public class FrameworkExtrasAutoConfiguration {
    // 引用 extras.config.*Factory 按功能装配：
    // LockFactory / RateLimitFactory / IdempotentFactory / StorageFactory / NotificationFactory
    // ExcelFactory / CaptchaFactory / Ip2RegionFactory / DesensitizeModuleFactory
    // OperateLogFactory / DataPermissionFactory（各自 @ConditionalOnProperty 独立开关）
}
```

#### 4.8 health/ — 健康检查聚合

##### 定位

把各模块 HealthIndicator 汇总为框架级健康信息。

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
@ConditionalOnProperty(prefix = "framework.health", name = "enabled", matchIfMissing = true)
public class FrameworkHealthAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FrameworkHealthAggregator frameworkHealthAggregator(List<HealthIndicator> indicators) { ...}
    // 聚合：cache/data/i18n/plugin 的 HealthIndicator，统一输出框架健康明细
}
```

#### 4.9 observability/ — 可观测性聚合

##### 定位

Micrometer 2.0 指标统一注册。

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
public class FrameworkObservabilityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FrameworkMeterBinderAggregator meterBinderAggregator(List<MeterBinder> binders) { ...}
    // 聚合 cache/data-jdbc/logger/i18n 的 MeterBinder，统一注册到全局 MeterRegistry
}
```

#### 4.10 web/ — Web 适配

##### 定位

各模块 Web 拦截器/过滤器的统一注册。

```textmate

@AutoConfiguration
@ConditionalOnWebApplication(type = SERVLET)
@ConditionalOnClass(name = "cn.jowen.framework.i18n.interceptor.I18nInterceptor")
public class FrameworkWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FrameworkWebMvcConfigurer frameworkWebMvcConfigurer(
            @Nullable I18nInterceptor i18nInterceptor,
            @Nullable I18nResponseInterceptor i18nResponseInterceptor,
            @Nullable RateLimitInterceptor rateLimitInterceptor) {
        // addInterceptors：i18n → 限流 → 幂等 → 操作日志（顺序注册）
    }
}
```

#### 4.11 bridge/ — 事件双向桥接（EventBridge）

##### 定位（冲突修正决议 #7）

打通 `core.event` 自研总线与 Spring `ApplicationEvent` 生态，两向互见、默认开启。

```textmate
cn.jowen.framework.boot.autoconfigure.bridge
├─FrameworkEventBridgeAutoConfiguration   #@ConditionalOnProperty("framework.event.bridge-enabled"，默认 true)
        ├─SpringToFrameworkBridge                  #
监听 ApplicationEvent →
转发 core
EventBus（按类型映射）
        ├─FrameworkToSpringBridge                  #
订阅 core
EventBus →
发布 ApplicationEvent
└─EventMappingRegistry                     #类型映射注册表（默认 1:1全量；可配置过滤）
```

**双向语义**：

- 宿主业务发 Spring 事件 → `SpringToFrameworkBridge` 转发 → 框架插件/模块经 `EventBus` 收到；
- 框架内 `EventBus.publish` → `FrameworkToSpringBridge` 发布 → Spring 监听器/`@EventListener` 收到；
- 防回环：Bridge 自身发布带来源标记（`EventSource.BRIDGE`），对端 Bridge 忽略；
- 配置过滤：`framework.event.bridge.include-packages / exclude-packages` 控制转发范围。

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────────────────────────────┐
│              framework-boot-autoconfigure                        │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │   META-INF/spring/...AutoConfiguration.imports             │  │
│  │   └─ cn.jowen.framework.boot.autoconfigure.FrameworkAutoConfig. │  │
│  └─────────────────────────┬─────────────────────────────────┘  │
│                            │ @AutoConfiguration                 │
│  ┌─────────────────────────▼─────────────────────────────────┐  │
│  │              FrameworkAutoConfiguration（总入口）           │  │
│  │  @ConditionalOnProperty("framework.enabled")              │  │
│  │  @Import(11 个装配类)                                     │  │
│  └──┬────────┬────────┬────────┬────────┬────────┬───────────┘  │
│     │        │        │        │        │        │              │
│  ┌──▼───┐ ┌──▼─────┐ ┌▼──────┐ ┌▼─────┐ ┌▼──────┐ ┌▼────────┐ │
│  │logger│ │ data   │ │ cache │ │ i18n │ │plugin │ │ extras  │ │
│  │Logger│ │DS/Jdbc│ │Cache- │ │MsgSrc│ │Plugin│ │委托 Extra│ │
│  │Bootstrap│Mybatis│ │Manager│ │Locale│ │Manager│ │sAutoCfg │ │
│  └──────┘ └────────┘ └───────┘ └──────┘ └──────┘ └─────────┘ │
│     │        │        │        │        │        │              │
│  ┌──▼───────┴──▼───────▼───────▼────────▼────────▼─────────┐  │
│  │  health（聚合 HealthIndicator）+ observability（聚合      │  │
│  │  MeterBinder）+ web（拦截器注册）                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  依赖汇聚点（唯一）                                        │  │
│  │  spring-boot-autoconfigure · spring-context(optional)     │  │
│  │  spring-expression(optional) · spring-boot-actuator(opt)  │  │
│  │  + framework-logger/data-*/cache/i18n/plugin/extras       │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0（零依赖）        framework-core
                      ▲
L1（依赖 L0）        framework-logger / framework-data-core
                      ▲
L2（依赖 L0+L1）     framework-data-jdbc / framework-data-mybatis
                    framework-cache / framework-i18n
                      ▲
L3（依赖 L0~L2）     framework-plugin / framework-extras
                      ▲
L4（依赖全部）       ★ framework-boot-autoconfigure（本模块，唯一的 Boot 依赖点）
                      ▲
L5（聚合）           framework-boot-starter（仅 POM）
```

**模块间规则**：

- 本模块 compile 依赖全部 L0~L3 模块 + `spring-boot-autoconfigure`；
- `spring-context` / `spring-expression` / `spring-boot-starter-actuator` 以 optional 引入，仅在对应装配类被触发时生效；
- 实现模块（logger/data-*/cache/i18n） **不依赖**本模块，保持单向：实现 → 装配层；
- 业务方只依赖 `framework-boot-starter`（POM 聚合本模块），不直接接触本模块类。

---

## 七、外部依赖

```xml

<dependencies>
    <!-- 框架内：全部实现模块（compile） -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-logger</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-core</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-jdbc</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-mybatis</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-cache</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-i18n</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-plugin</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-extras</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Boot 依赖汇聚点（核心） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 按需（optional） -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>       <!-- plugin 子容器 -->
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-expression</artifactId>    <!-- cache/extras SpEL -->
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- JSpecify 空安全 -->
    <dependency>
        <groupId>org.jspecify</groupId>
        <artifactId>jspecify</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  enabled: true                        # 框架总开关（matchIfMissing = true）

  logger:
    enabled: true
    mask-enabled: true
  data:
    enabled: true
    type: mybatis                      # jdbc / mybatis
    datasource:
      url: jdbc:mysql://localhost:3306/db
      username: root
      password: root
      hikari:
        maximum-pool-size: 20
        connection-timeout: 5000
  cache:
    enabled: true
    type: multi                        # local / redis / multi / none
  i18n:
    enabled: true
    default-locale: zh_CN
  plugin:
    enabled: false                     # 插件系统默认关闭
  extras:
    enabled: true
  health:
    enabled: true
    show-details: when-authorized
  observability:
    enabled: true
  web:
    enabled: true
```

---

## 九、使用方式

#### 9.1 业务方引入

```xml
<!-- 只需引入 Starter（内部依赖本模块） -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-boot-starter</artifactId>
</dependency>
        <!-- 按需补充实现 -->
<dependency>
<groupId>cn.jowen.framework</groupId>
<artifactId>framework-data-mybatis</artifactId>
</dependency>
```

#### 9.2 定制装配（用户覆写）

```textmate
// 方式一：@ConditionalOnMissingBean 让位 → 用户直接定义同类型 Bean
@Bean
public CacheManager customCacheManager() { ...}

// 方式二：Customizer 回调
@Bean
public CacheManagerCustomizer cacheManagerCustomizer() {
    return builder -> builder.defaultTtl(Duration.ofMinutes(10));
}

// 方式三：关闭单个模块
#framework.cache.enabled=false
```

#### 9.3 自定义装配类

```textmate
// 业务方新增自动装配：注册到 AutoConfiguration.imports 或通过 @ImportAutoConfiguration
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework", name = "enabled", matchIfMissing = true)
public class BizAutoConfiguration { ...
}
```

---

## 十、SPI 扩展点汇总

| 扩展点                                | 所在包        | 用途                                                        |
|:--------------------------------------|:--------------|:------------------------------------------------------------|
| 各模块 `*Customizer`                  | 对应装配类    | 定制 CacheManager / MessageSource / FlexGlobalConfig 等构建 |
| `@ConditionalOnMissingBean` 覆盖      | 全部装配类    | 用户 Bean 让位机制                                          |
| `FrameworkAutoConfigurationRegistrar` | 根包          | 编程式动态注册装配                                          |
| `FrameworkMeterBinderAggregator`      | observability | 自定义 MeterBinder 聚合                                     |
| `FrameworkHealthAggregator`           | health        | 自定义健康指标聚合策略                                      |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│                    业务应用（Application）                    │
│  只依赖 framework-boot-starter（POM）                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│   ★ framework-boot-autoconfigure（本模块）                   │
│   Boot 依赖唯一汇聚点 · AutoConfiguration.imports 注册       │
│   @Conditional* 按需装配 · Customizer 定制口                 │
│   health / observability / web 聚合                         │
└───────┬──────────────────────────────┬──────────────────────┘
        │ 装配                          │ 依赖
┌───────▼───────────────┐  ┌───────────▼──────────────────────┐
│  framework-logger     │  │  framework-data-*（jdbc/mybatis） │
│  framework-cache      │  │  framework-i18n / framework-plugin│
│  framework-extras     │  │  framework-core（地基）           │
└───────────────────────┘  └──────────────────────────────────┘
```

**依赖方向**：业务 → starter（POM）→ autoconfigure（装配）→ 各实现模块 → core（地基）。本模块是框架与 Spring Boot
的唯一耦合面，实现模块因此保持纯净，未来若替换装配机制（如 Quarkus CDI）只需重写本层。
