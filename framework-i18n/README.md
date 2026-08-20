# framework-i18n 模块架构设计

> 文档元信息
> - **模块**：framework-i18n
> - **关键词**：国际化、MessageSource、LocaleResolver、资源热加载、ScopedValue、消息格式化
> - **描述**：统一国际化与本地化基础设施，多语言消息管理、区域解析、消息格式化、多源聚合、动态刷新
> - **基线**：Spring Boot 4.x + Java 21（ScopedValue 上下文、虚拟线程、Micrometer 2.0）

---

## 一、模块定位

`framework-i18n` 是框架的 **国际化（i18n）与本地化（l10n）基础设施模块（L2）**
，提供多语言消息管理、区域解析、消息格式化、资源文件热加载与动态刷新能力。屏蔽底层不同消息源（Properties 文件 / 数据库 /
Redis）的差异，为上层业务提供统一 `MessageSource` 接口。

**核心价值**：

| 场景         | 没有本模块                               | 有本模块                                       |
|:-------------|:-----------------------------------------|:-----------------------------------------------|
| 多语言支持   | 业务代码 if/else 判断语言、硬编码字符串  | 统一 `MessageSource` 接口，代码整洁            |
| 动态文案更新 | 修改文案需重新打包部署重启               | 数据库/Redis 消息源修改即生效，无需重启        |
| 参数化消息   | `String.format` / 字符串拼接，易错难维护 | `{0}` `{1}` 占位符自动替换，支持命名参数与 ICU |
| 虚拟线程并发 | ThreadLocal 串扰                         | `ScopedValue` 上下文，结构化并发安全           |

**与核心模块的边界**：

| 模块               | 定位           | 特点                                   |
|:-------------------|:---------------|:---------------------------------------|
| **framework-i18n** | **国际化适配** | **统一消息接口、语言上下文、动态刷新** |
| framework-core     | 基础设施       | SPI、异常、断言                        |
| framework-cache    | 缓存抽象       | 可选集成加速消息读取                   |

---

## 二、功能清单与依赖矩阵

| 功能         | 子包        | 核心依赖             | 可选依赖                                                          |
|:-------------|:------------|:---------------------|:------------------------------------------------------------------|
| 核心抽象     | api         | framework-core       | —                                                                 |
| 消息资源存储 | source      | framework-core       | framework-data-jdbc（DB 源）/ framework-cache + Redis（Redis 源） |
| 区域解析     | locale      | —                    | jakarta.servlet-api（Web 场景）                                   |
| 消息格式化   | format      | —                    | ICU4J（高级格式化）                                               |
| 资源热加载   | reload      | framework-core Event | —                                                                 |
| 注解         | annotation  | —                    | spring-aop（切面）                                                |
| 拦截器       | interceptor | —                    | jakarta.servlet-api                                               |
| 事件         | event       | framework-core Event | —                                                                 |
| 配置与装配   | config      | Spring Boot 4        | spring-boot-configuration-processor                               |
| 工具         | support     | framework-core       | —                                                                 |

---

## 三、整体包结构

```text
framework-i18n
└─ src/main/java/com/framework/i18n/
   ├─ api/            # MessageSource / MessageSourceResolvable / LocaleResolver / I18nContext / I18nException
   ├─ source/         # AbstractMessageSource / PropertiesMessageSource / DatabaseMessageSource / RedisMessageSource / CompositeMessageSource / ResourceBundleMessageSource
   ├─ locale/         # AcceptHeaderLocaleResolver / CookieLocaleResolver / SessionLocaleResolver / ParameterLocaleResolver / FixedLocaleResolver / CompositeLocaleResolver / LocaleUtils
   ├─ format/         # MessageFormatter / JavaTextMessageFormatter（默认）/ NamedParameterMessageFormatter / IcuMessageFormatter / FormatterRegistry
   ├─ reload/         # ResourceWatcher / FileWatchResourceWatcher / DatabasePollingWatcher / RedisSubscriptionWatcher / ResourceReloader / ReloadStrategy
   ├─ annotation/     # @I18nMessage / @I18nField / @I18nException / @I18nLocale
   ├─ interceptor/    # I18nInterceptor / I18nFieldInterceptor / I18nExceptionInterceptor / I18nResponseInterceptor
   ├─ event/          # ResourceReloadedEvent / ResourceLoadFailedEvent / LocaleChangedEvent / I18nEventListener
   ├─ config/         # I18nAutoConfiguration / I18nProperties / SourceType / ResolverType / FormatterType / MessageSourceCustomizer
   └─ support/        # MessageCodeUtils / PropertiesFileParser / PlaceholderResolver / LocaleMatcher / I18nContextHolder
```

---

## 四、各子包详细设计

#### 4.1 api/ — 核心抽象

##### 定位

模块最顶层契约，所有实现必须遵循。

```textmate
cn.jowen.framework.i18n.api
├─ MessageSource                # 消息源核心接口
│  ├─ getMessage(String code) ->String                # 使用当前语言环境
│  ├─ getMessage(String code, Locale locale) ->String
│  ├─ getMessage(String code, Object[] args, Locale locale) ->String
│  └─ getMessage(MessageSourceResolvable resolvable, Locale locale) ->String
├─ MessageSourceResolvable      # 可解析消息引用（code +args +defaultMessage）
├─ LocaleResolver               # 区域解析器接口
│  ├─ resolveLocale(request) ->Locale
│  └─ setLocale(request, response, locale)
├─ I18nContext                  # 国际化上下文（基于 core  ContextCarrier，虚拟线程友好）
│  ├─ getCurrentLocale() ->Locale
│  ├─ getCurrentLocaleOrDefault() ->Locale
│  └─ <T> withLocale(Locale locale, Supplier<T> action)   # 作用域内执行
└─ I18nException                # 异常体系
    ├─ MessageNotFoundException  # 消息编码未找到
    ├─ ResourceLoadException     # 资源加载失败
    └─ FormatException           # 消息格式化失败
```

**上下文统一（冲突修正决议 # 3）**：`I18nContext` 的 Locale 读写委托 `core.context.ContextCarrier`（默认 ScopedValue 方案 A，
`framework.context.mode=threadlocal` 兼容方案 B）， **不再自带 ScopedValue 字段**；与多租户/数据权限/脱敏跳过/trace
共享同一载体，跨虚拟线程迁移统一走 `ContextSnapshot`。

```textmate
// 语义示例（实现细节在 core ContextCarrier）
public class I18nContext {
    public static final ContextKey<Locale> LOCALE = ContextKey.named("locale", Locale.class);

    public static Locale getCurrentLocale() {
        return ContextCarrier.get(LOCALE).orElse(Locale.getDefault());
    }

    public static <T> T withLocale(Locale locale, Supplier<T> action) {
        return ContextCarrier.runWith(LOCALE, locale, action);
    }
}
```

> **设计说明**：默认 `scopedvalue`（虚拟线程下无泄漏、无串扰、不可变）；`threadlocal` 兼容模式供依赖 ThreadLocal 的旧代码过渡，两种模式
> API 同形（core 保证），业务不感知。模式键为全局统一的 `framework.context.mode`（不再是 i18n 私有
> `framework.i18n.context-mode`）。

#### 4.2 source/ — 消息资源存储实现

##### 定位

不同来源的消息存储实现，全部继承 `AbstractMessageSource`。

```textmate
cn.jowen.framework.i18n.source
├─ AbstractMessageSource        # 抽象基类：解析流程 +参数化 +缓存
├─ PropertiesMessageSource      # Properties 文件（默认）：basenames +cacheSeconds +refresh()
├─ DatabaseMessageSource        # 数据库表：setDataSource /setSql /reload()
├─ RedisMessageSource           # Redis：Hash 存储 +订阅变更
├─ CompositeMessageSource       # 组合：多源聚合（按优先级回退）
└─ ResourceBundleMessageSource  # JDK ResourceBundle 适配
```

**多源聚合顺序**（`CompositeMessageSource`）：按配置的 `composite-order` 依次查找，先命中先返回；例如
`[DATABASE, PROPERTIES]` 表示数据库优先、文件兜底——适合"文件提供默认文案、DB 覆盖运营文案"的场景。

#### 4.3 locale/ — 区域解析

##### 定位

从请求/上下文解析当前 Locale。

```textmate
cn.jowen.framework.i18n.locale
├─ AcceptHeaderLocaleResolver    # HTTP Accept-Language（默认）
├─ CookieLocaleResolver          # Cookie 持久化用户语言偏好
├─ SessionLocaleResolver         # Session 绑定
├─ ParameterLocaleResolver       # URL 参数（?lang=en）
├─ FixedLocaleResolver           # 固定语言（测试/单语言环境）
├─ CompositeLocaleResolver       # 组合解析（按序回退：PARAMETER →COOKIE →ACCEPT_HEADER）
└─LocaleUtils                   # 
Locale 解析/匹配/规范化工具
```

#### 4.4 format/ — 消息格式化

##### 定位

占位符替换与格式化策略。

```textmate
cn.jowen.framework.i18n.format
├─ MessageFormatter              # 格式化器接口：format(String pattern, Object[] args, Locale locale)
├─ JavaTextMessageFormatter      # JDK MessageFormat（默认）：{0}{1}
├─ NamedParameterMessageFormatter#命名参数：{name}{age}
├─ IcuMessageFormatter           # ICU4J MessageFormat（复数/选择/日期本地化，可选）
└─FormatterRegistry             # 格式化器注册与按需分发
```

**版本要求**：默认实现仅依赖 JDK，零第三方；引入 ICU4J 后自动升级到高级格式化能力（复数规则、选择格式、区域化数字/日期）。

#### 4.5 reload/ — 资源热加载

##### 定位

资源变更的监听与重载，支持多策略。

```textmate
cn.jowen.framework.i18n.reload
├─ ResourceWatcher              # 监听器接口：start() / stop()
├─ FileWatchResourceWatcher     # 文件系统 WatchService 监听 *.properties 变更
├─ DatabasePollingWatcher       # 定时轮询 DB 变更版本号（默认 30s）
├─ RedisSubscriptionWatcher     # Redis Pub/Sub 订阅变更通知
├─ ResourceReloader             # 重载执行器（原子替换内存缓存）
└─ ReloadStrategy               # 策略枚举：POLLING /WATCH /SUBSCRIBE /MANUAL
```

**重载一致性**：重载期间新请求继续读取旧快照，重载完成后原子切换，避免读到半更新状态；发布 `ResourceReloadedEvent` 供监控。

#### 4.6 annotation/ — 国际化注解

##### 定位

声明式国际化入口。

```textmate
cn.jowen.framework.i18n.annotation
├─ @I18nMessage     # 标记方法/字段：value =消息编码
├─ @I18nField       # 返回值字段自动翻译（配合 I18nFieldInterceptor）
├─ @I18nException   # 异常消息国际化（code 映射）
└─ @I18nLocale      # 方法参数注入当前 Locale（如 @I18nLocale Locale locale）
```

#### 4.7 interceptor/ — 拦截器

##### 定位

Web 场景下的自动 Locale 解析与响应处理。

```textmate
cn.jowen.framework.i18n.interceptor
├─ I18nInterceptor             # 请求拦截 → LocaleResolver 解析 → 写入 I18nContext
├─ I18nFieldInterceptor        # 返回值字段自动翻译（@I18nField）
├─ I18nExceptionInterceptor    # 异常消息国际化（@I18nException /I18nException）
└─ I18nResponseInterceptor     # 响应头 Content-Language
```

#### 4.8 event/ — 事件

##### 定位

复用 framework-core 事件体系，可观测化。

```textmate
cn.jowen.framework.i18n.event
├─ ResourceReloadedEvent        # 资源重载完成（含 locale 与条目数）
├─ ResourceLoadFailedEvent      # 资源加载失败（含原因）
├─ LocaleChangedEvent           # 语言环境切换
└─ I18nEventListener            # 监听器接口（SPI 注册）
```

#### 4.9 config/ — 配置与装配

##### 定位

Spring Boot 4 自动装配入口。

```textmate
cn.jowen.framework.i18n.config
├─ I18nAutoConfiguration        # 装配 MessageSource /LocaleResolver /Interceptor /Watcher
├─ I18nProperties               # @ConfigurationProperties(prefix = "framework.i18n")
├─ SourceType                   # PROPERTIES /DATABASE /REDIS /COMPOSITE
├─ ResolverType                 # ACCEPT_HEADER /COOKIE /SESSION /PARAMETER /FIXED /COMPOSITE
├─ FormatterType                # JAVA_TEXT /NAMED_PARAMETER /ICU
└─ MessageSourceCustomizer      # 用户定制回调（多源注册、自定义格式化器）
```

**注册文件**：

```text
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
cn.jowen.framework.i18n.config.I18nAutoConfiguration
```

**GraalVM AOT 适配**：

```textmate
public class I18nRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources()
                .registerPattern("messages*.properties")
                .registerPattern("i18n/**/*.properties");
    }
}
```

**指标适配（Micrometer 2.0）**：`I18nMetricsCollector` 记录资源加载次数、消息命中率、重载耗时等指标。

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────────────────────────────┐
│                       framework-i18n                             │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              api（核心抽象，零 Spring）                     │  │
│  │  MessageSource ◄─── MessageSourceResolvable               │  │
│  │  LocaleResolver / I18nContext（ScopedValue）              │  │
│  └───────┬──────────────────────────┬───────────────────────┘  │
│          │ 实现                      │ 解析                      │
│  ┌───────▼──────────────┐  ┌────────▼───────────────┐        │
│  │  source（消息存储）  │  │  locale（区域解析）     │        │
│  │  PropertiesMessage   │  │  AcceptHeader/Cookie/  │        │
│  │  DatabaseMessage     │  │  Session/Parameter/    │        │
│  │  RedisMessage        │  │  Fixed/Composite       │        │
│  │  CompositeMessage    │  └────────┬───────────────┘        │
│  └───────┬──────────────┘           │                          │
│          │ 格式化                    │ 写入                      │
│  ┌───────▼──────────────┐  ┌────────▼───────────────┐        │
│  │  format（格式化）    │  │  interceptor（拦截）    │        │
│  │  JavaText / Named    │  │  I18nInterceptor ──→    │        │
│  │  Param / ICU         │  │  I18nContext            │        │
│  └──────────────────────┘  └────────┬───────────────┘        │
│                                     │ 变更触发                  │
│  ┌──────────────────────────────────▼───────────────────┐    │
│  │  reload（热加载） + event（事件）                      │    │
│  │  FileWatch / DatabasePolling / RedisSubscription     │    │
│  │  → ResourceReloadedEvent / LocaleChangedEvent        │    │
│  └──────────────────────────────────┬───────────────────┘    │
│                                     │                          │
│  ┌──────────────────────────────────▼───────────────────┐    │
│  │  config（装配，唯一依赖 Spring Boot 的层）             │    │
│  │  I18nAutoConfiguration ──→ I18nProperties            │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  framework-core（SPI/Event/Exception）· Spring Boot 4.x   │  │
│  │  虚拟线程（ScopedValue）· Micrometer 2.0 · GraalVM AOT     │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0（零依赖）        support / format（纯工具，零框架依赖）
                      ▲
L1（依赖 L0）        api / locale
                      ▲
L2（依赖 L1）        source（各实现）/ reload / event
                      ▲
L3（依赖 L0~L2）     annotation + interceptor（声明式入口）
                      ▲
L4（依赖 L0~L3）     config（自动装配，唯一依赖 Spring Boot 的层）
```

**模块间规则**：

- `api` 不依赖 Spring、不依赖 Web 容器；
- `interceptor` / `locale` 的 Web 实现依赖 `jakarta.servlet-api`（provided），无 Web 环境时自动降级为
  `FixedLocaleResolver`；
- 可选依赖 `framework-cache`（消息缓存）与 `framework-data-jdbc`（DB 消息源），均以 optional 方式声明，按需启用。

---

## 七、外部依赖

```xml

<dependencies>
    <!-- 框架内依赖 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>

    <!-- 可选：数据库消息源 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-jdbc</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 可选：Redis 消息源 / 订阅热加载 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-cache</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 可选：ICU4J 高级格式化 -->
    <dependency>
        <groupId>com.ibm.icu</groupId>
        <artifactId>icu4j</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Web 场景（provided，不强制） -->
    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Spring Boot 自动装配（仅 config 层） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
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
  i18n:
    enabled: true
    context-mode: scoped-value              # scoped-value / thread-local
    default-locale: zh_CN
    supported-locales: [ zh_CN, en_US, ja_JP ]
    source:
      type: composite                       # properties / database / redis / composite
      basenames: [ messages, validation ]
      composite-order: [ DATABASE, PROPERTIES ]   # 数据库优先、文件兜底
      cache-seconds: 60
      database:
        table-name: sys_i18n_message
        version-column: version             # 变更检测版本列（轮询用）
    locale:
      resolver-type: composite              # accept-header / cookie / session / parameter / fixed / composite
      resolver-order: [ PARAMETER, COOKIE, ACCEPT_HEADER ]
      cookie-name: language
      parameter-name: lang
    format:
      formatter-type: java-text             # java-text / named-parameter / icu
    reload:
      enabled: true
      strategy: auto                        # auto / polling / watch / subscribe / manual
      polling-interval: 30s
      debounce: 3s
    interceptors:
      enabled: true
      field-translation: true               # 返回值字段自动翻译
      response-content-language: true
```

---

## 九、使用方式

#### 9.1 引入依赖

```xml

<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-i18n</artifactId>
</dependency>
```

#### 9.2 编程式使用

```textmate

@Autowired
private MessageSource messageSource;

// 使用当前上下文语言环境
String msg = messageSource.getMessage("user.register.success");

// 指定语言 + 参数
String welcome = messageSource.getMessage(
        "welcome.message", new Object[]{"张三"}, Locale.US);

// ContextCarrier 作用域内切换语言（虚拟线程安全）
String jp = I18nContext.withLocale(Locale.JAPAN, () ->
        messageSource.getMessage("order.paid"));
```

#### 9.3 注解式使用（Web）

```textmate

@RestController
public class HelloController {

    // 自动解析语言（composite resolver），写入 I18nContext
    @GetMapping("/hello")
    public R<String> hello(@I18nLocale Locale locale) {
        return R.ok(messageSource.getMessage("hello"));
    }
}

// 返回值字段自动翻译
public class UserVO {
    private Long id;
    @I18nField("user.status")        // 按当前语言翻译
    private String status;
}
```

#### 9.4 异常消息国际化

```textmate

@I18nException(code = "error.order.not.found", args = {"#orderId"})
public class OrderNotFoundException extends I18nException {
    // 抛出后由 I18nExceptionInterceptor 翻译为当前语言文案
}
```

#### 9.5 动态更新（DB 消息源）

```textmate
// 运营在后台修改 sys_i18n_message 表 → DatabasePollingWatcher 检测版本变更
// → ResourceReloader 原子重载 → 新请求立即使用新文案，无需重启
```

---

## 十、SPI 扩展点汇总

| 扩展点接口                | 所在包 | 用途                                      |
|:--------------------------|:-------|:------------------------------------------|
| `MessageSourceCustomizer` | config | 定制消息源组合与格式化器                  |
| `MessageSource`           | api    | 全新消息源实现（如 Nacos/配置中心）       |
| `LocaleResolver`          | api    | 自定义区域解析策略（如从 Token/租户解析） |
| `MessageFormatter`        | format | 自定义消息格式化器                        |
| `ResourceWatcher`         | reload | 自定义资源变更监听（如 MQ 通知）          |
| `I18nEventListener`       | event  | 监听资源重载/加载失败/语言切换事件        |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│                    业务应用（Application）                  │
│  调用 MessageSource / @I18n* 注解获取多语言文案             │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-boot（适配编排层）                   │
│  FrameworkI18nAutoConfiguration                             │
│  ├─ MessageSource Bean（按 SourceType 装配）                │
│  ├─ LocaleResolver Bean（按 ResolverType 装配）             │
│  └─ Interceptor 注册 + Actuator 健康检查                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-i18n（本模块）                       │
│  MessageSource 接口 + Properties/DB/Redis/Composite 实现    │
│  ScopedValue 上下文 · 热加载 · 格式化 · 拦截器              │
└──────────┬──────────────────────────────┬───────────────────┘
           │                              │
┌──────────▼────────────┐  ┌──────────────▼──────────────────┐
│  framework-core       │  │  framework-cache / data-jdbc    │
│  SPI/Event/Exception  │  │  （可选：消息缓存 / DB 消息源） │
└───────────────────────┘  └─────────────────────────────────┘
```

**依赖方向**：业务 → i18n（API/注解）→ core（基础设施）。消息源与缓存、数据库解耦，按配置组合；ScopedValue 上下文与虚拟线程模型完全兼容，是框架
Java 21 基线的示范模块。
