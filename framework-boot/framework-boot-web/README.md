# framework-boot-web 模块设计

> **文档元信息**
> - **模块**：framework-boot-web
> - **关键词**：分布式锁、接口限流、幂等控制、验证码、数据权限、操作日志、字段加解密、请求签名、字段脱敏、Excel
> - **描述**：框架 Web 层工具集，提供锁/限流/幂等/验证码/数据权限/操作日志/加解密/签名/脱敏/Excel 等 10 项声明式能力
> - **基线**：Spring Boot 4.x + Java 21（虚拟线程适配）

> ⚠️ **本文档以 `src/main/java` 当前代码为准重写**。此前版本存在多处失真（模块名误写为 framework-extras-web、声称以 framework-cache 为存储底座——本模块 pom 中并无该依赖、声称锁/验证码/数据权限/操作日志为后续规划——实际均已实现），已于 2026-09-05 按实际代码逐处修正。

---

## 一、模块定位

`framework-boot-web` 是框架的 **Web 层能力模块**，为 Web 请求链路提供 10 项声明式能力：

| 能力 | 包 | 声明方式 | 默认状态 |
|:-----|:---|:---------|:---------|
| 分布式锁 | lock | `@Lockable` | 开 |
| 接口限流 | ratelimit | `@RateLimit` | 开 |
| 幂等控制 | idempotent | `@Idempotent` | 开 |
| 请求签名 | sign | `@Sign` + 拦截器 | 开 |
| 请求体加解密 | crypto | `@Encrypt` + Advice | 开 |
| 字段脱敏 | desensitize | `@Desensitized` | 关 |
| 验证码 | captcha | 编程式 `CaptchaService` | 关 |
| 数据权限 | datapermission | `@DataPermission` | 关 |
| 操作日志 | operatelog | `@OperateLog` | 关 |
| Excel 导入导出 | excel | `@ExcelExport` / `@ExcelImport` | 关 |

**核心价值**：全部能力以注解 + AOP 暴露，业务方零样板代码接入；存储/Redis 客户端通过 SPI 抽象注入，本模块不硬依赖任何具体实现。

---

## 二、关键设计决策

### 2.1 不依赖 framework-cache

本模块 **pom 中不含 framework-cache 依赖**，也不依赖任何 Redis 客户端（Jedis / Lettuce / Redisson）。这与"以 framework-cache 为存储底座"的旧表述不符，是有意为之的架构选择：

- **保持 L0 轻依赖**：boot-web 只依赖 `framework-extras-common` + `framework-core` + Spring Web + AspectJ，可被不引入缓存体系的应用单独使用。
- **Redis 能力走 SPI 注入**：`lock.RedisCommandExecutor` 屏蔽客户端差异（见 §4.1），由业务方或自动装配层注入具体实现。

### 2.2 单机兜底 + 可替换存储

幂等、限流、验证码三项能力均有**本地内存默认实现**，开箱即用；需要跨实例生效时替换为 Redis 实现即可，接口不变：

| 能力 | 本地默认实现 | 接口 | Redis 实现状态 |
|:-----|:------------|:-----|:--------------|
| 幂等 | `LocalIdempotentStore` | `IdempotentStore` | ⚠️ 待补（见文末「未完成项」） |
| 限流 | `RateLimiterManager`（4 种算法均进程内） | `RateLimiter` | ⚠️ 待补（见文末「未完成项」） |
| 验证码 | `CaptchaStore.InMemory` | `CaptchaStore` | ⚠️ 待补（见文末「未完成项」） |
| 分布式锁 | `LocalLock` | `Lock` | ✅ 已具备（注入 `RedisCommandExecutor` 即生效） |

### 2.3 配置前缀为 `framework.extras.web`

配置采用**双层绑定**（全框架统一模式，避免实现层反向依赖 Spring）：

```text
ExtrasWebProperties      (本模块 properties/，纯 POJO 基类，无 Spring 注解)
        ▲
        │ extends
        │
BootWebExtrasProperties  (framework-boot-autoconfigure，带 @ConfigurationProperties)
```

`BootWebExtrasProperties` 声明 `@ConfigurationProperties(prefix = "framework.extras.web")`，由
`WebExtrasAutoConfiguration` 的 `@EnableConfigurationProperties` 注册。

> ⚠️ **勘误**：`ExtrasWebProperties` 类内 Javadoc 曾写「对应前缀 `jowen.web`」，属错误表述；
> `CaptchaService` 等类只借用其嵌套的 `Captcha.CaptchaType` 枚举，与 Spring 绑定无关。
> 真实前缀以 `BootWebExtrasProperties` 为准，即 `framework.extras.web`。

---

## 三、整体包结构

```text
framework-boot-web
└─ src/main/java/cn/jowen/framework/extras/web/
   ├─ lock/               # 分布式锁（9 类）
   ├─ ratelimit/          # 接口限流（7 类）
   ├─ idempotent/         # 幂等控制（4 类）
   ├─ captcha/            # 验证码（9 类）
   ├─ crypto/             # 请求体加解密（4 类）
   ├─ sign/               # 请求签名（4 类）
   ├─ datapermission/     # 数据权限（5 类）
   ├─ operatelog/         # 操作日志（5 类）
   ├─ desensitize/        # 字段脱敏（4 类 + package-info）
   ├─ excel/              # Excel 导入导出（3 类 + package-info）
   ├─ properties/         # ExtrasWebProperties 配置属性
   ├─ exception/          # WebException
   └─ util/               # SpelUtils
```

> 注：包路径仍为 `cn.jowen.framework.extras.web`（历史沿用），模块名已是 `framework-boot-web`，两者不一致属历史遗留。

---

## 四、各子包详细设计

### 4.1 lock/ — 分布式锁

```text
├─ Lock                    # 接口：tryLock / unlock / isLocked / refresh
├─ LocalLock               # 进程内实现（兜底）
├─ LockType                # 枚举：REENTRANT / FAIR / READ / WRITE / MULTI / RED
├─ DistributedLock         # 分布式锁接口
├─ RedisDistributedLock    # Redis 实现（委托 RedisCommandExecutor）
├─ RedisCommandExecutor    # SPI：屏蔽 Jedis/Lettuce/StringRedisTemplate 差异
├─ @Lockable               # 声明式注解：key / waitTime / leaseTime / lockType
├─ LockAspect              # AOP 拦截器
└─ LockAcquireException    # 获取锁失败异常
```

**使用示例**：

```java
@Lockable(key = "'order:' + #orderId", waitTime = 5000, leaseTime = 30000)
public void processOrder(Long orderId) { /* ... */ }
```

**Redis 接入**：`RedisDistributedLock` 不引用任何 Redis 客户端，仅依赖 `RedisCommandExecutor`（`setIfAbsent` / `get` / `delete` / `deleteIfMatch`）。业务方注入的适配实现决定底层客户端，框架零强制依赖。

### 4.2 ratelimit/ — 接口限流

```text
├─ RateLimiter             # 接口：tryAcquire / getAvailablePermits
├─ RateLimiterManager      # 管理器：按 key 获取/创建限流器
├─ FixedWindowRateLimiter  # 固定窗口
├─ SlidingWindowRateLimiter# 滑动窗口
├─ LeakyBucketRateLimiter  # 漏桶
├─ TokenBucketRateLimiter  # 令牌桶（可选委托 Bucket4j）
└─ @RateLimit              # 注解：key / permits / period / algorithm
   └─ RateLimitAspect      # AOP 拦截器
```

- 4 种算法均为**进程内计数**，未依赖 Redis。集群维度限流见文末「未完成项」。
- `bucket4j-core:8.10.1` 为可选依赖，供令牌桶复用成熟算法实现。

### 4.3 idempotent/ — 幂等控制

```text
├─ @Idempotent             # 注解：key / ttl
├─ IdempotentAspect        # AOP 拦截器
├─ IdempotentStore         # 接口：tryMark(key, expire, unit) / remove(key)
└─ LocalIdempotentStore    # 本地实现（ConcurrentHashMap）
```

**使用示例**：

```java
@Idempotent(key = "#orderNo", ttl = 120)
@PostMapping("/order")
public Order createOrder(@RequestBody OrderRequest request) { /* ... */ }
```

`IdempotentStore.tryMark` 语义即「不存在则写入」，与 `RedisCommandExecutor.setIfAbsent` 一一对应，迁移 Redis 实现成本极低。

### 4.4 captcha/ — 验证码

```text
├─ Captcha                 # 验证码载体（record）
├─ CaptchaService          # 服务：generate / verify
├─ CaptchaGenerator        # 生成器接口
├─ GraphicCaptchaGenerator # 图形验证码
├─ ArithmeticCaptchaGenerator # 算术验证码
├─ SliderCaptchaGenerator  # 滑块验证码
├─ SmsCaptchaGenerator     # 短信验证码
├─ SmsCaptchaSender        # 短信发送 SPI
└─ CaptchaStore            # 存储接口 + InMemory 内存实现
```

### 4.5 crypto/ — 请求体加解密

```text
├─ @Encrypt                # 标注需加解密的控制器方法
├─ CryptoProcessor         # 接口
├─ AesGcmCryptoProcessor   # AES-256-GCM 实现
└─ CryptoAdvice            # 环绕控制器方法体执行加解密
```

密钥由配置注入：`jowen.web.crypto.keys`（别名 → base64 key 映射），`defaultKeyAlias` 指定默认密钥。

### 4.6 sign/ — 请求签名

```text
├─ @Sign                   # 标注需验签的方法
├─ SignInterceptor         # HandlerInterceptor：验签入口
├─ SignVerifier            # 接口：verify(params, signature)
└─ HmacSha256SignVerifier  # HMAC-SHA256 实现
```

App 密钥由 `jowen.web.sign.app-secrets`（appId → secret 映射）配置。

### 4.7 datapermission/ — 数据权限

```text
├─ @DataPermission         # 注解：enabled / deptColumn / userColumn / ignoreTables
├─ DataPermissionAspect    # AOP：绑定当前用户上下文
├─ DataPermissionContext   # 线程上下文：setCurrentUser / getCurrentUser
├─ DataPermissionRule      # 规则接口：getExpression(...)
└─ DataPermissionUserProvider # 当前用户获取 SPI
```

> 注：当前实现为「注解 + 上下文 + 规则接口」形态，SQL 改写由业务方实现的 `DataPermissionRule` 完成；**框架未内置 MyBatis 拦截器**（与旧文档描述不同）。

### 4.8 operatelog/ — 操作日志

```text
├─ @OperateLog             # 注解：module / action / description / content / condition
├─ OperateLogAspect        # AOP：采集请求上下文并派发事件
├─ OperateLogEvent         # 日志事件载体
├─ OperateLogHandler       # 处理器接口：handle(event)
└─ OperatorProvider        # 操作人获取 SPI
```

日志落地方式由业务方实现 `OperateLogHandler` 决定（日志输出 / 数据库 / MQ 均可），框架不内置具体落地实现。

### 4.9 desensitize/ — 字段脱敏

```text
├─ @Desensitized           # 标注需脱敏的字段或返回值
├─ DesensitizeAspect       # AOP 切面
└─ DesensitizeSupport      # 委托 core 的 Desensitizer 内核执行
```

脱敏规则模型与执行器由 `framework-core` 的 `Desensitizer` 统一提供，本包只做 Web 层适配，不重复实现规则。

### 4.10 excel/ — Excel 导入导出

```text
├─ @ExcelExport            # 导出注解
├─ @ExcelImport            # 导入注解
├─ ExcelExporter           # 导出器
├─ ExcelImporter           # 导入器
└─ ExcelException          # 异常
```

基于 `easyexcel`（optional 依赖）。`jowen.web.excel.max-rows` 限制单次行数，默认 100000，超限直接拒绝以防大结果集打爆内存。

---

## 五、配置属性

配置前缀 **`framework.extras.web`**，绑定类 `BootWebExtrasProperties`（extends `ExtrasWebProperties`），共 13 个配置块：

```yaml
framework:
  extras:
    web:
      enabled: true                    # 总开关
  
      sign:                            # 请求签名（默认开）
        enabled: true
        app-secrets: { app-001: "secret-001" }
  
      crypto:                          # 请求体加解密（默认开）
        enabled: true
        default-key-alias: default
        keys: { default: "<base64 32 bytes>" }
  
      rate-limit: { enabled: true }    # 限流（默认开）
      idempotent: { enabled: true }    # 幂等（默认开）
  
      lock:                            # 分布式锁（默认开）
        enabled: true
        default-lease-millis: 30000
  
      captcha:                         # 验证码（默认关）
        enabled: false
        type: ARITHMETIC               # GRAPHIC / ARITHMETIC / SLIDER / SMS
        expire-seconds: 120
        length: 4
        width: 120
        height: 40
  
      data-permission:                 # 数据权限（默认关）
        enabled: false
        dept-column: dept_id
        user-column: create_by
  
      operate-log:                     # 操作日志（默认关）
        enabled: false
        async: true
        handler: log
  
      notification: { enabled: false } # ⚠️ 仅配置存在，无对应实现包
      desensitize: { enabled: false }  # 字段脱敏（默认关）
      excel:                           # Excel（默认关）
        enabled: false
        max-rows: 100000
      ip-2-region: { enabled: false }  # IP 归属地，能力在 core 的 util/IpRegion
```

> 自动装配由 `framework-boot-autoconfigure` 的 `WebExtrasAutoConfiguration` 承载，不在本模块内。

---

## 六、外部依赖

```xml
<dependencies>
    <!-- 内部模块 -->
    <dependency><groupId>cn.jowen.framework</groupId><artifactId>framework-extras-common</artifactId></dependency>
    <dependency><groupId>cn.jowen.framework</groupId><artifactId>framework-core</artifactId></dependency>

    <!-- Spring Web -->
    <dependency><groupId>org.springframework</groupId><artifactId>spring-web</artifactId></dependency>
    <dependency><groupId>org.springframework</groupId><artifactId>spring-webmvc</artifactId></dependency>

    <!-- AOP -->
    <dependency><groupId>org.aspectj</groupId><artifactId>aspectjweaver</artifactId></dependency>

    <!-- 基础 -->
    <dependency><groupId>org.jspecify</groupId><artifactId>jspecify</artifactId></dependency>
    <dependency><groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId></dependency>
    <dependency><groupId>jakarta.servlet</groupId><artifactId>jakarta.servlet-api</artifactId><scope>provided</scope></dependency>

    <!-- 可选 -->
    <dependency><groupId>com.alibaba</groupId><artifactId>easyexcel</artifactId><optional>true</optional></dependency>
    <dependency><groupId>com.bucket4j</groupId><artifactId>bucket4j-core</artifactId><version>8.10.1</version></dependency>
</dependencies>
```

**无 framework-cache 依赖，无 Redis 客户端依赖**——这是本模块的设计约束，勿随意添加。

---

## 七、SPI 扩展点汇总

| 扩展点接口 | 所在包 | 用途 |
|:-----------|:-------|:-----|
| `Lock` / `DistributedLock` | lock | 自定义锁实现 |
| `RedisCommandExecutor` | lock | 接入 Redis 客户端（Jedis/Lettuce/StringRedisTemplate） |
| `RateLimiter` | ratelimit | 自定义限流算法 |
| `IdempotentStore` | idempotent | 自定义幂等存储 |
| `CaptchaGenerator` | captcha | 自定义验证码生成器 |
| `CaptchaStore` | captcha | 自定义验证码存储 |
| `SmsCaptchaSender` | captcha | 自定义短信发送通道 |
| `DataPermissionRule` | datapermission | 自定义数据权限规则 |
| `DataPermissionUserProvider` | datapermission | 获取当前用户 |
| `CryptoProcessor` | crypto | 自定义加解密算法 |
| `SignVerifier` | sign | 自定义签名校验算法 |
| `OperateLogHandler` | operatelog | 自定义操作日志落地 |
| `OperatorProvider` | operatelog | 获取操作人 |

---

## 八、未完成项

| 项 | 状态 | 说明 |
|:---|:-----|:-----|
| 幂等 Redis 实现 | ✅ 已完成 | `RedisIdempotentStore`（`a0081be`），基于 `RedisCommandExecutor.setIfAbsent`/`delete`；`tryMark` 语义与 `setIfAbsent` 等价，故复用而不扩展 SPI |
| 验证码 Redis 实现 | ✅ 已完成 | `RedisCaptchaStore`（`a0081be`），`\|` 分隔 + Base64 编码，`null` 以 `~` 标记 |
| 限流集群化 | ⚠️ 待补 | 4 种算法中仅固定窗口与令牌桶有 Redis 实现；滑动窗口（Redis ZSET）、漏桶（Lua）待 Redis 化 |
| `Notification` 配置块 | ✅ 已废弃 | `ExtrasWebProperties.Notification` 与 `NotificationProperties` 已标 `@Deprecated`（`a0081be`），保留不删（`E2004` 为协议级错误码，计划 1.0 移除） |
| MyBatis 数据权限拦截器 | ⚠️ 未内置 | 仅提供规则接口与上下文，SQL 改写需业务方实现 |
