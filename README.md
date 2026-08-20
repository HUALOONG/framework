# Jowen Framework 框架整体设计方案

> **基于 Spring Boot 4.x + Java 21 的完整架构设计**

---

## 一、背景与目标

### 1.1 背景

团队自研 Java 框架（`cn.jowen.framework`）沉淀多年，涵盖日志、数据访问、缓存、国际化、插件化、扩展工具等能力。随着 Spring Boot
4.x（Spring Framework 7）发布，需要一次 **整体基线升级**：统一技术栈到 Spring Boot 4.x + Java 21，拥抱虚拟线程与 AOT
原生镜像，同时保持 core 层纯净可测试。

### 1.2 目标

| 目标       | 说明                                                      |
|------------|-----------------------------------------------------------|
| 基线统一   | 全框架锁定 Spring Boot 4.x / Spring Framework 7 / Java 21 |
| 核心纯净   | core 层零 Spring 依赖，可脱离容器做单元测试               |
| 按需装配   | 模块化自动配置，`@Conditional` 按需生效，无多余 Bean      |
| 下游解耦   | BOM 只管框架模块版本，不绑架三方依赖                      |
| 云原生就绪 | GraalVM 原生镜像、虚拟线程、可观测性开箱即用              |

### 1.3 非目标

- 不兼容 Spring Boot 2.x 下游（备选方案另行处理）
- 不做代码生成器以外的代码脚手架
- 不包含具体业务实现

---

## 二、技术基线（Spring Boot 4.x + Java 21）

### 2.1 版本矩阵

| 技术栈           | 版本要求                 | 说明                                       |
|------------------|--------------------------|--------------------------------------------|
| **Java**         | **21 (LTS，强制)**       | **框架编译目标 = 21，不再下探**            |
| **Spring Boot**  | **4.x**（4.0.7 / 4.1.0） | 主方案基线                                 |
| Spring Framework | 7.x                      | Spring Boot 4.x 底层框架                   |
| Jakarta EE       | 11（Servlet 6.1）        | —                                          |
| Jackson          | 3.x                      | 包名 `tools.jackson`（非 com.fasterxml）   |
| JSpecify         | 1.0.0                    | 空安全注解，包级 `@NullMarked`             |
| Micrometer       | 2.0                      | 指标与上下文传播（ContextSnapshot）        |
| Tomcat           | 11+                      | Undertow 已移除                            |
| GraalVM          | 原生镜像                 | 生产级支持，AOT 编译                       |
| Caffeine         | ≥ 3.2.0                  | 已适配虚拟线程                             |
| Lettuce          | ≥ 6.4                    | 已适配虚拟线程                             |
| MyBatis Flex     | ≥ 1.11+                  | 需确认 Spring Boot 4.x 兼容（风险项）      |
| Maven            | 4.x                      | 注意同 Reactor 内 import 未安装 BOM 的限制 |

### 2.2 Java 21 落地特性（框架直接受益）

| JEP     | 特性                        | 框架应用点                                                                      |
|---------|-----------------------------|---------------------------------------------------------------------------------|
| JEP 444 | 虚拟线程（Virtual Threads） | 全框架 IO 路径：Servlet 容器、JDBC、Redis、日志异步写入；连接池参数重调         |
| JEP 446 | ScopedValue（预览）         | i18n 上下文、TraceContext、租户上下文：虚拟线程间安全传递，替代部分 ThreadLocal |
| JEP 453 | 结构化并发（预览）          | extras 聚合查询 / 并行任务编排可选使用                                          |
| JEP 441 | 模式匹配 switch             | 方言分发、异常分类、序列化器选择中替代 if-else 链                               |
| JEP 440 | Record Patterns             | EntityMetadata、配置模型（ConfigurationProperties 天然匹配）                    |
| JEP 439 | 分代 ZGC                    | 运行参数建议：大堆场景启用，低延迟                                              |
| JEP 430 | String Templates（预览）    | SQL 构建、日志模板可选演进（保守使用，避免语法依赖）                            |

> 原则： **强制基线为 Java 21 正式特性**；预览特性（ScopedValue / 结构化并发 / String Templates）仅在不影响下游编译的前提下渐进使用，默认不开。

### 2.3 Spring Boot 4.x 关键变化与应对

| Spring Boot 4.x 变化         | 影响                | 框架应对                                                    |
|------------------------------|---------------------|-------------------------------------------------------------|
| Spring Framework 7 基线      | 全部模块编译目标    | 统一升级，禁止旧 API                                        |
| Jakarta EE 11（Servlet 6.1） | web 模块、Tomcat 11 | autoconfigure 适配新 API                                    |
| Jackson 3（`tools.jackson`） | 所有 JSON 序列化    | 统一封装 `JsonMapper` 工厂，模块间禁止直接 new ObjectMapper |
| Micrometer 2.0               | 指标 API 变更       | 全部统计上报收敛到 2.0 API                                  |
| 虚拟线程默认启用             | 容器/连接池参数     | maximumPoolSize 10~30、connectionTimeout 5s                 |
| 模块化自动配置               | 自动配置加载机制    | 全部走 `AutoConfiguration.imports`，废弃 spring.factories   |
| GraalVM AOT 生产级           | 反射/资源需注册     | 每模块提供 `RuntimeHintsRegistrar`                          |
| Undertow 移除                | 容器选择            | 默认 Tomcat 11，可选 Jetty                                  |

---

## 三、总体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    业务应用 (Application)                   │
│            @SpringBootApplication + 引入 starter            │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-boot (适配编排层)                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  framework-boot-autoconfigure  (自动装配逻辑)        │   │
│  │  framework-boot-starter          (Starter 聚合入口)  │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              Spring Boot 4.x / Spring Framework 7           │
│  Jakarta EE 11 · JSpecify 1.0 · Jackson 3 · Micrometer 2.0  │
│  虚拟线程 · GraalVM 原生镜像 · 模块化自动配置 · 内置弹性    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                    框架能力模块层                           │
│  ┌────────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ framework-     │  │ framework-   │  │ framework-      │  │
│  │ data-core      │  │ cache        │  │ i18n            │  │
│  │ (数据访问抽象) │  │ (缓存管理)   │  │ (国际化)        │  │
│  └──────┬─────────┘  └──────────────┘  └─────────────────┘  │
│         │                                                   │
│  ┌──────┴───────────────────────┐                           │
│  │                              │                           │
│  ▼                              ▼                           │
│  ┌──────────────┐  ┌──────────────────┐                     │
│  │ framework-   │  │ framework-       │                     │
│  │ data-jdbc    │  │ data-mybatis     │                     │
│  │ (JDBC 实现)  │  │ (MyBatis Flex    │                     │
│  │              │  │  增强实现)       │                     │
│  └──────────────┘  └──────────────────┘                     │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ framework-   │  │ framework-   │  │ framework-       │   │
│  │ plugin       │  │ extras       │  │ logger           │   │
│  │ (插件化扩展) │  │ (扩展工具集) │  │ (日志管理)       │   │
│  └──────────────┘  └──────────────┘  └──────────────────┘   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              framework-core (基础设施)               │   │
│  │         SPI · Lifecycle · Exception · Assert         │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**版本治理（最终决策）**：

```
根 pom.xml（parent）          framework-bom（对外 BOM）
├─ properties：全部版本号     ├─ parent → 根 pom
├─ dependencyManagement       └─ dependencyManagement
│   ├─ spring-boot-deps          ├─ framework-core:${framework.version}
│   ├─ HikariCP / Redisson       ├─ framework-data-jdbc:${...}
│   └─ Jackson 等第三方库        └─ ... 仅框架自身模块
└─ modules（全部子模块）
```

- **根 pom.xml**：管理第三方依赖版本（Spring Boot、HikariCP、Redisson、Jackson…），通过继承链对框架内部子模块生效
- **framework-bom**：只声明框架自身模块版本（`${framework.version}`），供下游 `<scope>import</scope>` 引入
- **下游干净**：引入 BOM 只获得框架模块版本锁定，三方版本可自由选择

---

## 四、模块清单总览

| 模块                             | 定位                                    | 外部依赖                | Spring Boot 4.x 适配重点                |
|----------------------------------|-----------------------------------------|-------------------------|-----------------------------------------|
| **framework-bom**                | 版本仲裁（只管框架模块）                | 无                      | 继承根 pom，不混三方依赖                |
| **framework-core**               | 基础设施（SPI/异常/断言/生命周期/事件） | JSpecify                | 空注解                                  |
| **framework-logger**             | 日志管理（脱敏/追踪/结构化）            | Logback/Log4j2          | MDC 虚拟线程适配 + Micrometer 2.0       |
| **framework-data**               | 数据访问聚合父模块                      | —                       | 仅 POM 聚合                             |
| **framework-data-core**          | 数据访问抽象（零实现、零 Spring 依赖）  | 无（仅 framework-core） | 空注解                                  |
| **framework-data-jdbc**          | JDBC 轻量实现                           | HikariCP/Druid          | 连接池调优 + Jackson 3 + Micrometer 2.0 |
| **framework-data-mybatis**       | MyBatis Flex 增强实现                   | MyBatis Flex            | Flex 版本兼容 + Jackson 3 + AOT         |
| **framework-cache**              | 缓存管理（多级/防护/监控）              | Caffeine/Lettuce        | Jackson 3 + Micrometer 2.0 + Actuator   |
| **framework-i18n**               | 国际化（热加载/多源）                   | ICU4J(可选)             | ScopedValue + Micrometer 2.0            |
| **framework-plugin**             | 插件化扩展（隔离/热部署）               | framework-core          | 类加载隔离 + Spring 子容器              |
| **framework-extras**             | 扩展工具集（12 项按需引入）             | core/cache/data 等      | 见 §9.9                                 |
| **framework-boot-autoconfigure** | 自动装配逻辑                            | Spring Boot 4           | @AutoConfiguration + @Conditional       |
| **framework-boot-starter**       | Starter 聚合入口                        | 无（仅 POM）            | —                                       |

---

## 五、分层依赖规则

```
L0 (零依赖)        framework-core
                      ▲
L1 (依赖 L0)        framework-logger / framework-data-core
                      ▲
L2 (依赖 L0+L1)     framework-data-jdbc / framework-data-mybatis
                    framework-cache / framework-i18n
                      ▲
L3 (依赖 L0~L2)     framework-boot-autoconfigure
                    framework-extras / framework-plugin
                      ▲
L4 (聚合)           framework-boot-starter
```

**核心原则（第 25 轮明确）**：`*-core` 模块保持对 Spring Boot **零依赖/弱依赖**——只定义接口、抽象类、领域模型；Spring Boot 由
autoconfigure 层依赖 core，方向不可颠倒。这样 core 可脱离 Spring 做单元测试，版本升级也不绑架下游。

---

## 六、核心设计原则（三条关键决策）

1. **核心与实现分离**：core 层纯净（仅 JDK 21 + JSpecify），实现层（jdbc/mybatis/cache 等）依赖 core，autoconfigure 层负责桥接
   Spring。
2. **BOM 职责隔离**（第 36 轮最终决策）：framework-bom 只管理框架模块版本；第三方依赖版本统一在根 pom.xml 管理。BOM
   中不得混入三方坐标。
3. **Spring Boot 4.x + Java 21 全栈适配**：Jackson 3（`tools.jackson`）、Micrometer 2.0、虚拟线程（ScopedValue /
   ContextSnapshot）、GraalVM RuntimeHints、模块化自动配置，作为全部实现模块的共性基线。

---

## 七、Java 21 特性应用设计（框架级）

### 7.1 虚拟线程（JEP 444）——全框架 IO 重载

| 位置                  | 做法                                                                                        |
|-----------------------|---------------------------------------------------------------------------------------------|
| Web 容器              | Tomcat 11 虚拟线程执行器（Spring Boot 4.x 默认支持，`spring.threads.virtual.enabled=true`） |
| JDBC 连接池           | 连接是稀缺阻塞资源，虚拟线程不能无限放大：`maximumPoolSize` 10~30，`connectionTimeout` 5s   |
| Redis（Lettuce ≥6.4） | 原生虚拟线程安全，直接复用                                                                  |
| 异步日志              | 日志队列消费线程改为虚拟线程，规避高吞吐下线程耗尽                                          |
| 阻塞调用兜底          | 对第三方 SDK 阻塞调用提供 `VirtualThreadExecutor` 包装，限制并发数                          |

**关键约束**：虚拟线程下 **禁止同步锁阻塞池化线程**（如 synchronized 等待远程调用）；同步代码块只用于临界区短操作，否则引入钉扎（pinning）问题。

### 7.2 ScopedValue（JEP 446，预览）——上下文传递

| 上下文                               | 现状              | 演进                                                                              |
|--------------------------------------|-------------------|-----------------------------------------------------------------------------------|
| TraceContext（logger）               | ThreadLocal + MDC | 主用 Micrometer ContextSnapshot（Spring Boot 4.x 标准），ScopedValue 作为增强实验 |
| I18nContext（i18n）                  | ThreadLocal       | **默认改用 ScopedValue**，虚拟线程间天然隔离、零泄漏                              |
| TenantContext（data-mybatis/extras） | ThreadLocal       | 预留 ScopedValue 适配点                                                           |
| 缓存防击穿标志                       | ThreadLocal       | 保持 ThreadLocal（短临界区，无跨线程需求）                                        |

> 预览特性默认关：编译开启 `--enable-preview` 仅限 i18n 模块内部，下游不感知。若生产验证良好，待 ScopedValue 正式化后全框架铺开。

### 7.3 Record / 模式匹配——代码结构现代化

- 配置模型：全部 `@ConfigurationProperties` 用 `record` 声明（不可变、equals/hashCode 免费）
- 方言分发：`switch (dbType)` 模式匹配替代 if-else 链
- 异常分类：`switch (sqlState)` 匹配 SqlStateClassifier 结果
- 序列化器选择：`switch (serializerType)` 直接选择策略

### 7.4 分代 ZGC（JEP 439）

- 框架提供 `JVM_OPTS` 推荐模板：`-XX:+UseZGC -XX:+ZGenerational -Xlog:gc*`
- 不强制，仅作为文档化建议

---

## 八、各模块详细设计

### 8.1 framework-core — 基础设施层

**定位**：零依赖基础层，提供 SPI 扩展机制、异常体系、断言工具、生命周期管理、事件机制、通用工具。唯一外部依赖 JSpecify。

```
cn.jowen.framework.core/
├─ spi/          ExtensionLoader<T> · @SPI · @Activate
├─ exception/    FrameworkException · BusinessException · SystemException · ErrorCode
├─ lifecycle/    Lifecycle · SmartLifecycle · LifecycleProcessor
├─ assert_/      Assert · State
├─ event/        Event · EventListener<E>
└─ util/         ClassUtils · StringUtils · CollectionUtils · ReflectionUtils
```

**Java 21 / Spring Boot 4.x 适配**：包级 `@NullMarked` 空安全标记；`ClassUtils` 优先用 `MethodHandles`/反射内省替代暴力反射；编译目标
`release=21`。

---

### 8.2 framework-logger — 日志管理

**定位**：统一日志门面，提供日志脱敏、链路追踪增强、异步日志、结构化日志。

```
cn.jowen.framework.logger/
├─ facade/      Logger · LoggerFactory · LogLevel
├─ adapter/     LoggerAdapter · LogbackAdapter · Log4j2Adapter
├─ trace/       TraceEnhancer · TraceContext · MdcContextPropagation
├─ mask/        SensitiveDataMasker · MaskRule · MaskPattern（手机号/身份证/银行卡）
├─ layout/      StructuredLayout · JsonLogFormatter
└─ config/      LoggerProperties
```

**Spring Boot 4.x 适配**：

- MDC 上下文传播改用 Micrometer ContextSnapshot（虚拟线程切换不丢）
- 异步日志消费线程用虚拟线程
- 提供 `LoggerRuntimeHints`（GraalVM 反射注册）

---

### 8.3 framework-data-core — 数据访问抽象

**定位**：数据访问纯抽象层，定义仓储、查询、分页、事务、方言、异常体系。 **零 JDBC 依赖、零 Spring 依赖**（第 25 轮确认）。

```
cn.jowen.framework.data.core/
├─ repository/    Repository<T,ID> · CrudRepository · PagingRepository
│                 DynamicRepository · RepositoryFactory(SPI)
├─ query/         QueryWrapper<T> · UpdateWrapper · Condition · Operator · JoinType
├─ page/          Page<T> · PageRequest · Pageable
├─ sort/          Sort · Order · Direction · NullHandling
├─ transaction/   TransactionManager(SPI) · TransactionTemplate · Propagation · Isolation
├─ datasource/    DataSource · DataSourceProperties · DataSourceRouter · PoolType
├─ mapping/       EntityMetadata · RowMapper(SPI) · TypeHandler · NamingStrategy · EntityScanner(SPI)
├─ dialect/       DatabaseDialect(SPI) · DatabaseType · DialectDetector(SPI)
├─ callback/      EntityCallback · ConnectionCallback · StatementCallback
├─ exception/     DataAccessException 体系（DuplicateKey/OptimisticLock/...）+ ExceptionTranslator(SPI)
└─ support/       EntityBase<ID> · VersionedEntity<ID> · AuditableEntity<ID>
```

**Java 21**：EntityMetadata 使用 Record 承载；异常分类用模式匹配 switch。

---

### 8.4 framework-data-jdbc — JDBC 实现

**定位**：data-core 的轻量 JDBC 实现，封装 SQL 构建执行、结果集映射、连接池、事务、方言。

```
cn.jowen.framework.data.jdbc/
├─ core/          JdbcTemplate · NamedParameterTemplate · BatchTemplate · SqlRunner
├─ connection/    ConnectionProvider · HikariConnectionProvider · DruidConnectionProvider
├─ statement/     PreparedStatementBuilder · SqlBuilder · ParameterBinder
├─ mapping/       BeanPropertyRowMapper · DefaultTypeHandlers(15+) · CamelCaseNamingStrategy
├─ dialect/       MySQLDialect · PostgreSQLDialect · OracleDialect · SQLServerDialect · H2Dialect
├─ transaction/   JdbcTransactionManager · TransactionSynchronizationManager
├─ repository/    JdbcRepository · SimpleJdbcRepository · IdGenerator（自增/UUID/雪花/Sequence）
├─ interceptor/   SqlInterceptor · LoggingInterceptor · PerformanceInterceptor · TenantInterceptor
├─ exception/     SQLExceptionTranslator · SqlStateClassifier
├─ config/        JdbcProperties · DataSourceConfiguration
└─ util/          JdbcUtils · ResultSetExtractor · LobHandler
```

**Spring Boot 4.x / Java 21 适配**：

- 虚拟线程下连接池 `maximumPoolSize` 建议 10~30，`connectionTimeout` 缩短至 5s
- JsonTypeHandler 用 Jackson 3 不可变 `JsonMapper`
- SQL 指标适配 Micrometer 2.0 Timer；提供 `JdbcRuntimeHints`
- SqlStateClassifier 用模式匹配 switch 分类

---

### 8.5 framework-data-mybatis — MyBatis Flex 增强实现

**定位**：data-core 的 MyBatis Flex 桥接实现，提供类型安全查询、多表关联、脱敏、多租户、SQL 审计等企业能力； **保留原生
`xxxMapper.xml`**（第 10 轮确认）。

```
cn.jowen.framework.data.mybatis/
├─ adapter/       FlexRepositoryAdapter · FlexTransactionAdapter · FlexExceptionTranslator
├─ repository/    FlexRepository · FlexJoinRepository · FlexDynamicRepository
├─ query/         FlexQueryWrapperTranslator · FlexLambdaQueryBuilder
├─ extension/     FlexAuditHandler · FlexMaskProcessor · FlexEncryptProcessor
│                 FlexTenantHandler · FlexSqlAuditListener · FlexLogicDeleteHandler
├─ exception/     FlexExceptionConverter
├─ config/        MybatisFlexAutoConfiguration · MybatisFlexProperties
└─ codegen/       EntityGenerator · MapperGenerator · TableDefGenerator（可选）
```

**Spring Boot 4.x 适配**：MyBatis Flex ≥ 1.11+（需确认 Spring Boot 4.x 自动配置兼容）；`MybatisRuntimeHints` 注册 Mapper 反射与
`mapper/**/*Mapper.xml` 资源；审计处理器 JSON 用 Jackson 3。

---

### 8.6 framework-cache — 缓存管理

**定位**：统一缓存，多级缓存（L1 Caffeine + L2 Redis）、缓存注解、统计监控、防穿透/击穿/雪崩。

```
cn.jowen.framework.cache/
├─ api/           Cache<K,V> · CacheManager · CacheConfig · CacheException
├─ local/         CaffeineCache（默认）· ConcurrentMapCache · LoadingLocalCache
├─ distributed/   RedisCache · MemcachedCache
├─ multi/         MultiLevelCache · CacheSyncBroadcaster（Redis Pub/Sub 跨节点同步）
├─ serialization/ JsonCacheSerializer(Jackson 3) · KryoCacheSerializer · ProtobufCacheSerializer
├─ annotation/    @Cacheable · @CachePut · @CacheEvict · @CacheInvalidate · @Caching
├─ interceptor/   CacheInterceptor · CacheKeyGenerator · CacheExpressionEvaluator(SpEL)
├─ event/         CacheHitEvent · CacheMissEvent · CachePutEvent · CacheEvictEvent
├─ stats/         CacheStatsCollector · CacheStatsReporter(Micrometer 2.0) · CacheHealthIndicator
├─ config/        CacheAutoConfiguration · CacheProperties · CacheType(LOCAL/REDIS/MULTI/NONE)
└─ support/       CachePenetrationShield · CacheBreakdownShield · CacheAvalancheShield · NullValue
```

**Spring Boot 4.x 适配**：JsonCacheSerializer 用 Jackson 3；统计上报 Micrometer 2.0；`CacheHealthIndicator` 注册到
Actuator。

---

### 8.7 framework-i18n — 国际化

**定位**：多语言消息管理、区域解析、消息格式化、资源热加载、动态刷新。

```
cn.jowen.framework.i18n/
├─ api/           MessageSource · LocaleResolver · I18nContext · I18nException
├─ source/        PropertiesMessageSource · DatabaseMessageSource · RedisMessageSource
│                 CompositeMessageSource（多源聚合）
├─ locale/        AcceptHeader/Cookie/Session/Parameter/Fixed/Composite LocaleResolver
├─ format/        JavaTextMessageFormatter（默认）· NamedParameterMessageFormatter · IcuMessageFormatter
├─ reload/        FileWatchResourceWatcher · DatabasePollingWatcher · RedisSubscriptionWatcher
├─ annotation/    @I18nMessage · @I18nField · @I18nException
├─ interceptor/   I18nInterceptor · I18nFieldInterceptor · I18nExceptionInterceptor
├─ event/         ResourceReloadedEvent · LocaleChangedEvent
├─ config/        I18nAutoConfiguration · I18nProperties
└─ support/       MessageCodeUtils · PlaceholderResolver · LocaleMatcher · I18nContextHolder
```

**Spring Boot 4.x / Java 21 适配**：I18nContext 用 **ScopedValue**（虚拟线程友好，兼容 ThreadLocal 可切换）；
`I18nRuntimeHints` 注册
`messages*.properties` 资源。

---

### 8.8 framework-plugin — 插件化扩展

**定位**：插件发现、加载、类加载隔离、生命周期管理、依赖解析、插件间通信。依赖 framework-core，可选 framework-logger；自身不依赖
Spring，通过 autoconfigure 桥接。

**包结构**：api / descriptor / loader / resolver / lifecycle / registry / extension / context / event / hotswap /
config / support

**核心设计**：

- **类加载隔离**（FRAMEWORK_API_DELEGATE 推荐策略）：框架 API 委派宿主 → 共享库委派 SharedClassLoader →
  插件私有类自身加载 → 隐藏类（如 javax.servlet.\*\*）拒绝
- **Spring 子容器**：每个插件一个子 ApplicationContext，parent = 宿主容器，可访问宿主 Bean，卸载时 close 销毁
- **热部署**：WatchService 监听 plugins 目录，新 jar 自动加载、删除自动卸载、更新自动重启（防抖 3s）
- **扩展机制**：`@ExtensionPoint` / `@Extension` / `@ExtensionScan` + plugin.json 描述符双通道
- **依赖解析**：DAG 拓扑排序 + 版本仲裁（VersionRange 交集）+ 循环检测
- **Actuator 集成**：`/actuator/plugins` 端点（start/stop/restart/install/uninstall）+ PluginHealthIndicator

```yaml
framework:
  plugin:
    enabled: true
    plugins-dir: ./plugins
    hot-swap: { enabled: true, strategy: restart, debounce-interval: 3s }
    class-loading:
      strategy: framework-api-delegate
      exported-packages: [ cn.jowen.framework.core.**, cn.jowen.framework.plugin.api.** ]
      shared-libraries: [ com.fasterxml.jackson.core:jackson-databind ]
    spring: { enabled: true }
```

> 注意：插件内部如需 JSON 序列化，应依赖框架统一封装的 `tools.jackson` 版本，避免与宿主类加载器版本冲突。

---

### 8.9 framework-extras — 扩展工具集

**定位**：12 项场景化能力，全部按需引入、独立开关（统一 `framework.extras.<feature>.enabled`）。

| 子包           | 能力        | 说明                                                                                     |
|----------------|-------------|------------------------------------------------------------------------------------------|
| lock           | 分布式锁    | Redis（Lettuce+Lua）可重入/公平/读写/联锁/红锁，Watchdog 自动续期，`@Lockable`           |
| ratelimit      | 接口限流    | 固定窗口/滑动窗口/漏桶/令牌桶，集群维度基于 Redis，`@RateLimit`                          |
| idempotent     | 幂等控制    | Token 模式 / Key 模式，防重复提交，`@Idempotent`                                         |
| storage        | 文件存储    | 本地 / MinIO / 阿里云 OSS / AWS S3 统一抽象，`@StorageFile`                              |
| notification   | 消息通知    | 邮件 / 短信 / 钉钉 / 企业微信 / Webhook                                                  |
| excel          | Excel 处理  | 基于 EasyExcel，声明式导入导出、流式大数据、模板填充、多 Sheet                           |
| captcha        | 验证码      | 图形 / 算术 / 滑块 / 短信验证码，生成与校验                                              |
| ip2region      | IP 地域解析 | 离线库纯内存查询                                                                         |
| desensitize    | 数据脱敏    | Jackson 序列化扩展，注解声明式字段脱敏（Jackson 3 适配）                                 |
| operatelog     | 操作日志    | 注解自动记录，异步写入（虚拟线程），自定义模板                                           |
| datapermission | 数据权限    | MyBatis 拦截器/SQL 改写，行级：部门/个人/自定义规则                                      |
| config         | 配置与装配  | 各功能 `@ConfigurationProperties` 与 Bean 工厂，统一 `framework.extras.<feature>.*` 开关 |

---

### 8.10 framework-boot-autoconfigure — 自动装配

**定位**：模块化自动配置，`@AutoConfiguration` + `@Conditional` 按需装配。

```
cn.jowen.framework.boot.autoconfigure/
├─ FrameworkAutoConfiguration           # 总入口（@Import 各模块）
├─ logger/    FrameworkLoggerAutoConfiguration
├─ data/      DataSourceAutoConfiguration · DataJdbcAutoConfiguration · DataMybatisAutoConfiguration
├─ cache/     FrameworkCacheAutoConfiguration
├─ i18n/      FrameworkI18nAutoConfiguration
├─ plugin/    FrameworkPluginAutoConfiguration
├─ extras/    FrameworkExtrasAutoConfiguration
├─ health/    FrameworkHealthAutoConfiguration
├─ observability/  FrameworkObservabilityAutoConfiguration
└─ web/       FrameworkWebAutoConfiguration
```

**注册文件**：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**装配示例**：

```textmate

@AutoConfiguration
@ConditionalOnClass(name = "cn.jowen.framework.data.jdbc.core.JdbcTemplate")
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "framework.data", name = "type", havingValue = "jdbc")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class DataJdbcAutoConfiguration { ...
}
```

---

### 8.11 framework-boot-starter — 聚合入口

**定位**：仅含 pom.xml，无代码。用户引入一个依赖获得框架核心能力（core + autoconfigure + logger +
spring-boot-starter），数据访问实现按需另引。

```xml

<dependencies>
    <dependency>spring-boot-starter</dependency>
    <dependency>framework-core</dependency>
    <dependency>framework-logger</dependency>
    <dependency>framework-boot-autoconfigure</dependency>
</dependencies>
```

---

### 8.12 framework-bom — 版本仲裁

**定位**：纯 POM 聚合器，无代码、无 SPI。

**最终决策（第 36-37 轮）**：

- `<parent>` 指向根 pom.xml，继承 `${framework.version}` 等属性， **不重复声明 properties**
- `<dependencyManagement>` 只声明框架自身模块（`cn.jowen.framework:framework-*:${framework.version}`）
- **禁止混入** HikariCP、Redisson 等第三方坐标（否则下游 import 被锁版本）
- 根 pom 只做 `<dependencyManagement>`（版本字典），不声明 `<dependencies>`
- Maven 4 注意：同 Reactor 内 import 未安装 BOM 会被禁止，开发期先 `mvn install -pl framework-bom`

---

### 8.13 framework-data（聚合父模块）与 framework-boot（超级聚合器）

- **framework-data**：纯聚合 POM，聚合 data-core / data-jdbc / data-mybatis 三个子模块
- **framework-boot**：超级聚合器，聚合框架全部子模块；呈现 Layer -1（外部技术栈）到 Layer 4（starter）的完整依赖树与全局配置

---

## 九、跨模块共性适配清单

| 适配项           | 影响模块                                         | 做法                                                                                       |
|------------------|--------------------------------------------------|--------------------------------------------------------------------------------------------|
| Java 21 编译     | 全部模块                                         | `<maven.compiler.release>21</maven.compiler.release>`，禁止降级编译                        |
| JSpecify 空安全  | 全部模块                                         | 每个包 `@NullMarked`，可空返回值标 `@Nullable`                                             |
| Jackson 3 迁移   | data-jdbc / cache / data-mybatis / extras        | `com.fasterxml.jackson` → `tools.jackson`，`ObjectMapper` → `JsonMapper.builder().build()` |
| 虚拟线程适配     | logger / data-jdbc / data-mybatis / cache / i18n | ThreadLocal → ScopedValue 或 Micrometer ContextSnapshot                                    |
| Micrometer 2.0   | logger / data-jdbc / cache / i18n                | 统计上报统一 Micrometer 2.0 API                                                            |
| GraalVM 原生镜像 | 所有有实现模块                                   | 每模块提供 `RuntimeHintsRegistrar`                                                         |
| 连接池调优       | data-jdbc / data-mybatis                         | 虚拟线程下 maximumPoolSize 10~30、connectionTimeout 5s                                     |
| Actuator 集成    | cache / data-jdbc / plugin / extras              | `HealthIndicator` + `MeterBinder` + 自定义 `@Endpoint`                                     |

---

## 十、非功能需求

### 10.1 性能

| 项     | 目标                               | 手段                                      |
|--------|------------------------------------|-------------------------------------------|
| 吞吐   | 虚拟线程下高并发 IO 不因线程数受限 | Spring Boot 4.x 虚拟线程 + 连接池参数重调 |
| 延迟   | P99 优先                           | 分代 ZGC + 日志/缓存异步化                |
| 冷启动 | 原生镜像 ≤ 2s                      | GraalVM AOT + 最小化反射扫描              |

### 10.2 安全

- 日志脱敏默认开启（手机号/身份证/银行卡）
- MyBatis 字段加解密（FlexEncryptProcessor）+ SQL 注入防护（预编译强制）
- 插件类加载隐藏 `javax.*` 危险包
- 统一依赖漏洞扫描（根 pom 接入 OWASP Dependency-Check）

### 10.3 可观测性

- 全模块 Micrometer 2.0 指标：JVM、连接池、缓存命中率、SQL 耗时、插件状态
- 日志结构化 JSON + TraceId 贯通（ContextSnapshot）
- Actuator 端点：health / metrics / plugins / cache / i18n

### 10.4 兼容性

- 只支持 Spring Boot 4.x + Java 21；旧版本由附录备选方案覆盖
- 下游 `import framework-bom` 后三方版本自由

---

## 十一、统一配置属性（application.yml 示例）

```yaml
framework:
  enabled: true                       # 框架总开关

  logger:
    enabled: true
    mask-enabled: true                # 日志脱敏

  data:
    enabled: true
    type: mybatis                     # jdbc / mybatis
    datasource:
      url: jdbc:mysql://localhost:3306/db
      username: root
      password: root
      hikari:
        maximum-pool-size: 20         # 虚拟线程下建议 10~30
        connection-timeout: 5000      # 5s 快速失败
    mybatis:
      audit-enabled: true
      mask-enabled: true
      tenant-enabled: true
      mapper-locations: classpath*:/mapper/**/*Mapper.xml

  cache:
    enabled: true
    type: multi                       # local / redis / multi / none
    defaults: { ttl: 30m, max-size: 10000, value-serializer: json }
    redis: { host: 127.0.0.1, port: 6379, key-prefix: "myapp:cache:" }

  i18n:
    enabled: true
    default-locale: zh_CN
    supported-locales: [ zh_CN, en_US, ja_JP ]
    source: { type: composite, basenames: [ messages, validation ] }

  plugin:
    enabled: false                    # 插件系统默认关闭
    plugins-dir: ./plugins

  extras:
    lock: { enabled: true }
    ratelimit: { enabled: true }
    # ... 其余功能独立开关

  health:
    enabled: true
    show-details: when-authorized

spring:
  threads:
    virtual:
      enabled: true                   # 启用虚拟线程
  task:
    execution:
      propagate-context: true         # 上下文透传（Boot 4.1+）
  main:
    keep-alive: true                  # 防止全 daemon 虚拟线程导致 JVM 退出

# JVM 推荐参数（生产）
# java -XX:+UseZGC -XX:+ZGenerational -Xmx4g -Xlog:gc* -jar app.jar
```

---

## 十二、使用方式

### 12.1 引入依赖

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-data-mybatis</artifactId>
</dependency>
</dependencies>
```

### 12.2 启动类

```textmate

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 12.3 编程式示例（缓存 + 仓储 + 国际化）

```textmate

@Service
public class OrderService {
    @Cacheable(key = "#id", ttl = "10m")
    public Order getById(Long id) {
        return orderRepository.findById(id).orElseThrow(() ->
                new BusinessException(i18n.getMessage("order.not_found", id)));
    }

    @ExtrasLock(key = "'order:pay:'+#id", waitTime = "3s")
    public void pay(Long id) { ...}
}
```

---

## 十三、标准文档模板（11 章，第 20-22 轮确定）

| 章节                   | 内容                                             |
|------------------------|--------------------------------------------------|
| 文档元信息             | 模块名称、标题、关键词、描述                     |
| 一、模块定位           | 职责 + 核心价值表（有/无对比）+ 与核心模块边界表 |
| 二、功能清单与依赖矩阵 | 功能/子包/核心依赖/可选依赖                      |
| 三、整体包结构         | 树形目录                                         |
| 四、各子包详细设计     | 每子包按「定位 → 包结构 → 使用示例」三段式       |
| 五、核心类关系图       | ASCII 图                                         |
| 六、分层依赖规则       | 模块分层树                                       |
| 七、外部依赖           | pom.xml 配置                                     |
| 八、配置属性           | application.yml 示例                             |
| 九、使用方式           | 注解 + 编程式示例                                |
| 十、SPI 扩展点汇总     | 扩展点/所在包/用途                               |
| 十一、与整体框架的关系 | ASCII 模块依赖图                                 |

---

## 十四、演进路线与里程碑

| 里程碑          | 内容                                   | 出口标准                                                 |
|-----------------|----------------------------------------|----------------------------------------------------------|
| M1 基线搭建     | 根 pom + bom + core + logger 落地      | 空应用可启动，日志脱敏生效                               |
| M2 数据访问     | data-core + data-jdbc 实现             | JDBC CRUD + 事务 + 分页通过测试                          |
| M3 MyBatis 集成 | data-mybatis + Flex 适配               | Flex 查询/审计/多租户可用；**验证 Spring Boot 4.x 兼容** |
| M4 缓存与 i18n  | cache + i18n 落地                      | 多级缓存命中率可观测；i18n 热加载生效                    |
| M5 扩展与插件   | extras + plugin                        | 12 项能力按需可用；插件热部署验证                        |
| M6 云原生       | AOT 原生镜像 + 虚拟线程压测 + 可观测性 | 全模块 RuntimeHints 齐备，原生镜像启动 ≤ 2s，压测达标    |
| M7 文档与发布   | 各模块 11 章文档 + BOM 发布            | 全部模块文档齐备，BOM 发布到私服                         |

---

## 十五、风险与待办

| 事项                              | 说明                                                          |
|-----------------------------------|---------------------------------------------------------------|
| MyBatis Flex Spring Boot 4.x 兼容 | 需确认 ≥ 1.11 支持 Spring Boot 4 自动配置，必要时先做兼容验证 |
| Jackson 3 迁移成本                | `tools.jackson` 包名变更影响所有 JSON 相关模块，需统一封装    |
| 虚拟线程 + 连接池                 | 连接池参数需按虚拟线程场景重新压测调优                        |
| GraalVM AOT                       | 各模块 RuntimeHints 需逐一完善并做原生镜像构建验证            |
| ScopedValue 预览特性              | 依赖 JDK 预览开关，正式化前仅限 i18n 模块内部使用             |
| 代码骨架                          | 各模块设计已定稿，尚未生成代码骨架                            |
