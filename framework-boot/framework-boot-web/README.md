# framework-extras-web 模块架构设计

> 文档元信息
> - **模块**：framework-extras-web
> - **关键词**：分布式锁、接口限流、幂等控制、验证码、数据权限、操作日志
> - **描述**：框架 Web 层工具集，提供锁/限流/幂等/验证码/数据权限/操作日志等 6 项高频能力，复用 framework-cache 作为存储底座
> - **基线**：Spring Boot 4.x + Java 21（虚拟线程适配、Micrometer 2.0）

---

## 一、模块定位

`framework-extras-web` 是框架的 **Web 层工具集模块**，提供 6 项高频能力：分布式锁、接口限流、幂等控制、验证码、数据权限、操作日志。全部能力基于 `framework-cache` 作为存储底座，通过 AOP 实现声明式使用。

**核心价值**：

| 场景         | 没有本模块                         | 有本模块                           |
|:-------------|:-----------------------------------|:-----------------------------------|
| 分布式锁     | 各业务自行封装 Redis 锁            | @Lockable 声明式，Watchdog 自动续期|
| 接口限流     | 自行实现限流算法                   | 4 种算法可选，集群维度基于 Redis   |
| 幂等控制     | 重复提交导致数据异常               | @Idempotent 自动校验，Token/Key 双模式 |
| 验证码       | 自行实现图形/滑块验证码            | 4 种类型，自动存储校验             |
| 数据权限     | 手写 SQL WHERE 条件                | @DataPermission 自动改写 SQL       |
| 操作日志     | 手动记录操作日志                   | @OperateLog 自动异步记录           |

---

## 二、功能清单与依赖矩阵

| 功能         | 子包           | 核心依赖           | 可选依赖       |
|:-------------|:---------------|:-------------------|:---------------|
| 接口限流     | ratelimit      | framework-core     | Bucket4j（可选） |
| 幂等控制     | idempotent     | framework-core     | Redis（后续）   |
| 字段加解密   | crypto         | framework-core     | —              |
| 请求签名     | sign           | framework-core     | —              |

> 说明：本模块当前实现限流、幂等、加解密、签名四类能力的抽象与单机兜底；分布式锁、验证码、数据权限、操作日志等为后续规划，不在本次实现范围。

---

## 三、整体包结构

```text
framework-extras-web
└─ src/main/java/cn/jowen/framework/extras/web/
   ├─ ratelimit/             # 接口限流：@RateLimit / RateLimiter / TokenBucketRateLimiter
   ├─ idempotent/            # 幂等控制：@Idempotent / IdempotentStore / LocalIdempotentStore
   ├─ crypto/                # 字段加解密：@Encrypt / CryptoProcessor / AesGcmCryptoProcessor
   ├─ sign/                  # 请求签名：@Sign / SignVerifier / HmacSha256SignVerifier
   ├─ properties/            # ExtrasWebProperties 配置属性
   └─ exception/             # WebException
```

## 四、各子包详细设计

### 4.1 lock/ — 分布式锁

```text
cn.jowen.framework.extras.web.lock
├─ Lock                           # 接口：tryLock / unlock / isLocked
├─ DistributedLock                # 分布式锁接口
├─ DistributedLockManager         # 管理器：getLock / getFairLock / getReadLock / getWriteLock
├─ RedisDistributedLock           # Redis 实现：Lettuce + Lua，可重入/公平/读写
├─ LocalDistributedLock           # 本地实现（单机兜底）
├─ LocalLock                      # 进程内锁
├─ @Lockable                      # 声明式注解：key / waitTime / leaseTime / lockType
├─ LockInterceptor                # AOP 拦截器
├─ LockType                       # 枚举：REENTRANT / FAIR / READ / WRITE / MULTI / RED
└─ LockException                  # 异常
```

**使用示例**：

```java
@Lockable(key = "'order:' + #orderId", waitTime = 5000, leaseTime = 30000)
public void processOrder(Long orderId) { ... }
```

### 4.2 ratelimit/ — 接口限流
```text
cn.jowen.framework.extras.web.ratelimit
├─ RateLimiter                    # 接口：tryAcquire / getAvailablePermits
├─ RateLimiterManager             # 管理器：getLimiter(key, config)
├─ algorithm/
│  ├─ FixedWindowRateLimiter      # 固定窗口：Redis INCR + EXPIRE
│  ├─ SlidingWindowRateLimiter    # 滑动窗口：Redis ZSET
│  ├─ LeakyBucketRateLimiter      # 漏桶：Redis Lua
│  └─ TokenBucketRateLimiter      # 令牌桶：Redis Lua
├─ @RateLimit                     # 注解：key / permits / period / algorithm / scope
├─ RateLimitInterceptor           # AOP 拦截器
├─ RateLimitAlgorithm             # 枚举：FIXED_WINDOW / SLIDING_WINDOW / LEAKY_BUCKET / TOKEN_BUCKET
├─ RateLimitScope                 # 枚举：GLOBAL / USER / IP / CUSTOM
└─ RateLimitException             # 异常
```

**使用示例**：

```java
@RateLimit(key = "'api:user:list'", permits = 100, period = 60000, scope = USER)
public List<User> listUsers() { ... }
```

### 4.3 idempotent/ — 幂等控制
```text
cn.jowen.framework.extras.web.idempotent
├─ IdempotentValidator            # 接口：validate / mark / remove
├─ RedisIdempotentValidator       # Redis 实现：SETNX + EXPIRE
├─ LocalIdempotentValidator       # 本地实现（ConcurrentHashMap）
├─ @Idempotent                    # 注解：key / ttl / mode(TOKEN/KEY)
├─ IdempotentInterceptor          # AOP 拦截器
├─ IdempotentTokenGenerator       # Token 生成器：UUID / Snowflake
└─ IdempotencyException           # 异常
```

**使用示例**：

```java
@Idempotent(mode = IdempotentMode.TOKEN, ttl = 120000)
@PostMapping("/order")
public Order createOrder(@RequestBody OrderRequest request) { ... }
```

### 4.4 captcha/ — 验证码

```text
cn.jowen.framework.extras.web.captcha
├─ CaptchaService                 # 服务：generate / verify
├─ CaptchaResult                  # 结果：captchaId / image(Base64) / expiresIn
├─ CaptchaType                    # 枚举：IMAGE / ARITHMETIC / SLIDER / SMS
├─ generator/
│  ├─ CaptchaGenerator            # SPI 接口
│  ├─ ImageCaptchaGenerator       # 图形验证码
│  ├─ ArithmeticCaptchaGenerator  # 算术验证码
│  └─ SliderCaptchaGenerator      # 滑块验证码
├─ store/
│  ├─ CaptchaStore                # SPI 接口
│  ├─ CacheCaptchaStore           # framework-cache 实现
│  └─ LocalCaptchaStore           # 本地实现
└─ CaptchaException               # 异常
```

**使用示例**：

```java
// 生成
CaptchaResult result = captchaService.generate(CaptchaType.IMAGE);

// 校验
boolean valid = captchaService.verify(result.getCaptchaId(), "ABCD");
```

### 4.5 datapermission/ — 数据权限
```text
cn.jowen.framework.extras.web.datapermission
├─ DataPermission                 # 注解：enabled / deptColumn / userColumn / ignoreTables
├─ DataPermissionRule             # SPI 接口：getExpression(TableInfo, msId)
├─ rule/
│  ├─ DeptDataPermissionRule     # 部门规则：ALL / DEPT_AND_CHILD / DEPT / SELF
│  ├─ UserDataPermissionRule     # 本人规则
│  └─ CustomDataPermissionRule   # 自定义规则
├─ DataPermissionInterceptor      # MyBatis 拦截器
├─ DataPermissionContext          # 上下文：setCurrentUser / getCurrentUser
├─ UserInfo                       # 用户信息：userId / deptId / deptIds / dataScope
├─ DataScope                      # 枚举：ALL / DEPT_AND_CHILD / DEPT / SELF / CUSTOM
└─ DataPermissionException        # 异常
```

**使用示例**：

```java
@DataPermission(deptColumn = "dept_id", userColumn = "create_by")
public interface OrderMapper extends BaseMapper<Order> { }
```

### 4.6 operatelog/ — 操作日志
```text
cn.jowen.framework.extras.web.datapermission
cn.jowen.framework.extras.web.operatelog
├─ OperateLog                     # 注解：module / action / description / content / condition / async
├─ OperateLogRecord               # 记录：traceId / module / action / operator / requestParams / responseData / costTime
├─ OperateLogHandler              # SPI 接口：handle(OperateLogRecord)
├─ handler/
│  ├─ LoggingOperateLogHandler   # 默认：日志输出
│  ├─ DatabaseOperateLogHandler  # 数据库写入
│  └─ MqOperateLogHandler        # MQ 发送（预留）
├─ OperateLogInterceptor          # AOP 拦截器
├─ OperateLogContext              # 上下文：setOperator / getOperator
├─ OperateStatus                  # 枚举：SUCCESS / FAIL
└─ OperateLogDispatcher           # 异步分发器
```

**使用示例**：

```java
@OperateLog(module = "用户管理", action = "CREATE", description = "'新增用户: ' + #user.username")
public void createUser(User user) { ... }
```

---

## 五、核心类关系图

```text
┌────────────────────────────────────────────┐
│          framework-extras-web              │
│                                            │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐ │
│  │  lock    │  │ratelimit │  │idempotent │ │
│  │ @Lockable│  │@RateLimit│  │@Idempotent│ │
│  └────┬─────┘  └────┬─────┘  └────┬──────┘ │
│       │             │             │        │
│       └┬────────────┴─────────────┘        │
│        │                                   │
│    framework-cache                         │
│    (Lock/Idempotent/Captcha)               │
│                                            │
│  ┌──────────────────────────────────┐      │
│  │         captcha                  │      │
│  │  CaptchaService / Generator      │      │
│  └──────────────────────────────────┘      │
│                                            │
│  ┌──────────────────────────────────┐      │
│  │      datapermission              │      │
│  │  @DataPermission / Interceptor   │      │
│  │  (MyBatis 拦截器)                │      │
│  └──────────────────────────────────┘      │
│                                            │
│  ┌──────────────────────────────────┐      │
│  │         operatelog               │      │
│  │  @OperateLog / Interceptor       │      │
│  │  (AOP + 异步写入)                │      │
│  └──────────────────────────────────┘      │
│                                            │
│  依赖：framework-extras-common             │
│  依赖：framework-cache (可选)              │
│  依赖：framework-core                      │
└────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0 (零内部依赖)    framework-extras-web
                     依赖：framework-extras-common + framework-core
                     可选：framework-cache（lock/idempotent/captcha 需要）
```

---

## 七、外部依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-extras-common</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>

    <!-- 缓存底座（可选：lock/idempotent/captcha 需要） -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-cache</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- AOP（可选） -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-aop</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.aspectj</groupId>
        <artifactId>aspectjweaver</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- SpEL（可选） -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-expression</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 装配（可选） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  extras:
    lock:
      enabled: true
      type: redis                    # local / redis
      key-prefix: "lock:"
      default-lease-time: 30s
      default-wait-time: 10s
      watchdog-enabled: true
    ratelimit:
      enabled: true
      default-algorithm: sliding-window
      key-prefix: "ratelimit:"
      fallback-message: "请求过于频繁"
    idempotent:
      enabled: true
      default-ttl: 60s
      key-prefix: "idempotent:"
      token-header: X-Idempotent-Token
    captcha:
      enabled: true
      type: arithmetic
      length: 4
      ttl: 5m
    datapermission:
      enabled: true
      default-dept-column: dept_id
      default-user-column: create_by
      ignore-tables: [sys_config]
    operatelog:
      enabled: true
      async: true
      handler: database
      max-param-length: 2000
```

---

## 九、使用方式

```java
// 锁 + 限流 + 幂等 + 操作日志 组合使用
@RateLimit(key = "'api:order:create'", permits = 10, period = 60000, scope = USER)
@Idempotent(mode = IdempotentMode.TOKEN, ttl = 120000)
@OperateLog(module = "订单", action = "CREATE", description = "'下单: ' + #request.orderNo")
@Lockable(key = "'order:' + #request.orderNo", waitTime = 5000)
public Order createOrder(OrderCreateRequest request) { ... }
```

## 十、SPI 扩展点汇总

| 扩展点接口            | 所在包            | 用途                 |
|:----------------------|:------------------|:---------------------|
| `DistributedLock`     | lock              | 自定义锁实现         |
| `RateLimiter`         | ratelimit         | 自定义限流算法       |
| `IdempotentValidator` | idempotent        | 自定义幂等校验       |
| `CaptchaGenerator`    | captcha/generator | 自定义验证码生成器   |
| `CaptchaStore`        | captcha/store     | 自定义验证码存储     |
| `DataPermissionRule`  | datapermission    | 自定义数据权限规则   |
| `OperateLogHandler`   | operatelog        | 自定义操作日志处理器 |


---

## 十一、与整体框架的关系

```text
framework-extras-web
├─ 依赖：framework-extras-common（Properties + Exception）
├─ 依赖：framework-core（SPI / Event / ContextCarrier）
├─ 可选：framework-cache（lock / idempotent / captcha 存储底座）
└─ 可选：spring-aop / aspectj（AOP 拦截器）
```
