# framework-extras 模块架构设计

> 文档元信息
> - **模块**：framework-extras
> - **关键词**：分布式锁、接口限流、幂等控制、文件存储、消息通知、Excel、验证码、IP 地域、脱敏、操作日志、数据权限
> - **描述**：企业级工具集，12 项高频能力按需引入、互不耦合，统一自动装配
> - **基线**：Spring Boot 4.x + Java 21（Jackson 3、Micrometer 2.0、虚拟线程）

---

## 一、模块定位

`framework-extras` 是框架的 **企业级工具集模块（L3）**
，提供实际业务开发中高频使用、但不属于核心框架能力的增强功能。每个功能独立封装、按需启用、互不耦合，避免业务方重复造轮子，同时保持轻量——不用的功能不引入任何额外依赖。

**与核心模块的边界**：

| 模块                 | 定位       | 特点                       |
|:---------------------|:-----------|:---------------------------|
| framework-core       | 基础设施   | SPI、异常、断言            |
| framework-data-*     | 数据访问   | 仓储、查询、事务           |
| framework-cache      | 缓存管理   | 多级缓存、注解             |
| framework-i18n       | 国际化     | 消息源、区域解析           |
| **framework-extras** | **工具集** | **按需引入，每个功能独立** |

**关键设计原则**：

- **单一模块、多开关**：12 项功能共处一个模块，通过 `framework.extras.<feature>.enabled` 独立开关；
- **可选依赖隔离**：功能所需第三方 SDK（MinIO/OSS/S3/EasyExcel/ICU 等）全部 optional，未引入则不装配；
- **依赖收敛**：lock/ratelimit/idempotent/captcha 复用 framework-cache 作为存储底座，不重复引入 Redis 客户端。

---

## 二、功能清单与依赖矩阵

| 功能        | 子包           | 核心依赖             | 可选依赖                            |
|:------------|:---------------|:---------------------|:------------------------------------|
| 分布式锁    | lock           | framework-cache      | Redis（Lettuce）                    |
| 接口限流    | ratelimit      | framework-cache      | Redis（Lettuce）                    |
| 幂等控制    | idempotent     | framework-cache      | Redis（Lettuce）                    |
| 文件存储    | storage        | —                    | MinIO / 阿里云 OSS / AWS S3         |
| 消息通知    | notification   | —                    | Jakarta Mail / 短信 / 钉钉 / 企微   |
| Excel 处理  | excel          | —                    | EasyExcel                           |
| 验证码      | captcha        | framework-cache      | —                                   |
| IP 地域解析 | ip2region      | —                    | ip2region 离线库                    |
| 数据脱敏    | desensitize    | —                    | Jackson 3（序列化集成）             |
| 操作日志    | operatelog     | framework-core Event | —                                   |
| 数据权限    | datapermission | —                    | MyBatis（拦截器模式）               |
| 配置与装配  | config         | Spring Boot 4        | spring-boot-configuration-processor |
| 工具        | support        | framework-core       | —                                   |

---

## 三、整体包结构

```text
framework-extras
└─ src/main/java/com/framework/extras/
   ├─ lock/            # 分布式锁：DistributedLock / DistributedLockManager / RedisDistributedLock / LocalDistributedLock / @Lockable / LockInterceptor / LockType / LockProperties
   ├─ ratelimit/       # 接口限流：RateLimiter / RateLimiterManager / algorithm/* / @RateLimit / RateLimitInterceptor / RateLimitAlgorithm / RateLimitScope / RateLimitProperties
   ├─ idempotent/      # 幂等控制：IdempotentValidator / RedisIdempotentValidator / LocalIdempotentValidator / @Idempotent / IdempotentInterceptor / IdempotentTokenGenerator / IdempotentMode / IdempotentProperties
   ├─ storage/         # 文件存储：FileStorage / FileInfo / FileStorageManager / impl/* / strategy/* / @StorageConfig / StorageProperties
   ├─ notification/    # 消息通知：NotificationService / NotificationRequest / NotificationResult / NotificationChannel / channel/* / template/* / NotificationProperties
   ├─ excel/           # Excel 处理：ExcelService / ExcelExportRequest / ExcelImportRequest / ExcelImportResult / ExcelError / ExcelRowValidator / annotation/* / converter/* / handler/* / ExcelProperties
   ├─ captcha/         # 验证码：CaptchaService / CaptchaResult / CaptchaType / generator/* / store/* / CaptchaProperties
   ├─ ip2region/       # IP 地域解析：IpRegionService / IpRegion / IpRegionSearcher / Ip2RegionProperties
   ├─ desensitize/     # 数据脱敏：annotation/* / DesensitizeType / handler/* / serializer/* / condition/* / DesensitizeProperties
   ├─ operatelog/      # 操作日志：@OperateLog / OperateLogRecord / OperateLogHandler / handler/* / OperateLogInterceptor / OperateLogContext / OperateStatus / OperateLogProperties
   ├─ datapermission/  # 数据权限：@DataPermission / DataPermissionRule / rule/* / DataPermissionInterceptor / DataPermissionContext / UserInfo / DataScope / DataPermissionProperties
   ├─ config/          # 统一配置与自动装配：ExtrasAutoConfiguration / ExtrasProperties / 各功能 AutoConfiguration
   └─ support/         # 跨模块编排工具（定位见下，不含通用工具复制）
```

---

## 四、各子包详细设计

#### 4.1 lock/ — 分布式锁

##### 定位

基于缓存模块的分布式锁，支持可重入、公平、读写、联锁（MultiLock）、红锁（RedLock）。

```textmate
cn.jowen.framework.extras.lock
├─ DistributedLock            # 接口：lock / 

lock(waitTime) / tryLock / 

tryLock(wait, lease) / unlock / isLocked / isHeldByCurrentThread / forceUnlock
├─ DistributedLockManager     # 管理器：getLock / getFairLock / getReadLock / getWriteLock / getMultiLock / getRedLock
├─ RedisDistributedLock       # Redis 实现：Lettuce + Lua 脚本；可重入（Hash lockCount）；Watchdog 自动续期（默认 30s）；公平锁（List 等待队列）
├─ LocalDistributedLock       # 本地实现（单机兜底）：ReentrantLock / ReadWriteLock
├─ @Lockable                  # 声明式注解：key（SpEL）/waitTime / leaseTime / lockType / failMessage / fallback
├─ LockInterceptor            # 拦截器：解析注解 → SpEL 算 key → 加锁 → 执行 → 释放；失败抛 LockException 或走降级
├─ LockType                   # REENTRANT / FAIR / READ / WRITE / MULTI / RED
└─ LockProperties             # enabled / type / keyPrefix（"lock:"）/defaultLeaseTime（30s）/defaultWaitTime（10s）/watchdogEnabled
```

**使用示例**：

```textmate
// 声明式
@Lockable(key = "'order:' + # orderId", waitTime = 5000, leaseTime = 30000)
public void processOrder(Long orderId) { ...}

// 编程式
DistributedLock lock = lockManager.getLock("order:123");
if(lock.tryLock(5,TimeUnit.SECONDS)){try{...}finally{lock.unlock();}}
```

#### 4.2 ratelimit/ — 接口限流

##### 定位

接口级限流，支持四种算法、集群/本机两种维度。

```textmate
cn.jowen.framework.extras.ratelimit
├─ RateLimiter                 # 接口：tryAcquire / tryAcquire(permits) / tryAcquire(timeout) / getAvailablePermits
├─ RateLimiterManager          # 管理器：getLimiter(key, config) / removeLimiter(key)
├─ algorithm/                  # 算法实现
│  ├─ FixedWindowRateLimiter      # 固定窗口：Redis INCR +EXPIRE
│  ├─ SlidingWindowRateLimiter    # 滑动窗口：Redis ZSET（score=时间戳）
│  ├─ LeakyBucketRateLimiter      # 漏桶：Redis Lua
│  └─ TokenBucketRateLimiter      # 令牌桶：Redis Lua
├─ @RateLimit                  # key（SpEL）/permits / period / algorithm / scope / message / fallback
├─ RateLimitInterceptor        # 拦截器：按 scope 算 key（GLOBAL/USER/IP/CUSTOM）→tryAcquire →放行或拒绝
├─ RateLimitAlgorithm          # FIXED_WINDOW / SLIDING_WINDOW / LEAKY_BUCKET / TOKEN_BUCKET
├─ RateLimitScope              # GLOBAL / USER / IP / CUSTOM
└─ RateLimitProperties         # enabled / defaultAlgorithm / keyPrefix / fallbackMessage
```

**使用示例**：

```textmate
@RateLimit(key = "'api:user:list'", permits = 100, period = 1000, algorithm = SLIDING_WINDOW)
public List<User> listUsers() { ...}

@RateLimit(key = "'api:order:create'", permits = 10, period = 60000, scope = USER)
public Order createOrder(OrderRequest request) { ...}
```

#### 4.3 idempotent/ — 幂等控制

##### 定位

防止重复提交，支持 Token 模式与 Key 模式。

```textmate
cn.jowen.framework.extras.idempotent
├─ IdempotentValidator           # 接口：validate / mark / remove
├─ RedisIdempotentValidator      # SETNX + EXPIRE 原子操作
├─ LocalIdempotentValidator      # ConcurrentHashMap +定时清理（单机兜底）
├─ @Idempotent                   # key（SpEL）/ttl / message / mode（TOKEN/KEY）/tokenHeader
├─ IdempotentInterceptor         # TOKEN：请求头取 token →校验缓存存在 →删除 →放行；KEY：SETNX 标记；异常回滚标记
├─ IdempotentTokenGenerator      # UUID / Snowflake 生成
├─ IdempotentMode                # TOKEN / KEY
└─ IdempotentProperties          # enabled / defaultTtl（60s）/ keyPrefix / tokenHeader（X-Idempotent-Token）
```

**使用示例**：

```textmate

@Idempotent(mode = TOKEN, ttl = 120000)
@PostMapping("/order")
public Order createOrder(@RequestBody OrderRequest request) { ...}

@Idempotent(key = "#userId + ':' + # request.orderNo", ttl = 300000)
public void processPayment(Long userId, PaymentRequest request) { ...}
```

#### 4.4 storage/ — 文件存储

##### 定位

统一文件存储抽象，屏蔽后端差异，支持本地/MinIO/OSS/S3。

```textmate
cn.jowen.framework.extras.storage
├─ FileStorage                 # 接口：upload(InputStream/MultipartFile) / download / getPresignedUrl / delete / deleteBatch / exists / getFileInfo / listObjects
├─ FileInfo                    # objectName / originalFilename / contentType / size / etag / lastModified / url / metadata
├─ FileStorageManager          # 多后端管理：getStorage() / getStorage(name) / registerStorage
├─ impl/                       # LocalFileStorage（按日期分目录）/MinioFileStorage / AliyunOssFileStorage（STS）/AwsS3FileStorage（SDK v2）
├─ strategy/                   # ObjectNameStrategy：DatePath / Hash / OriginalName / Uuid\
├─ @StorageConfig              # 多存储后端 Bean 声明
└─ StorageProperties           # type / local / minio / aliyunOss / awsS3 / objectNameStrategy
```

**使用示例**：

```textmate
FileInfo info = storageManager.getStorage().upload(file);
String url = storageManager.getStorage().getPresignedUrl(objectName, Duration.ofHours(1));
```

#### 4.5 notification/ — 消息通知

##### 定位

统一通知抽象：邮件、短信、钉钉、企业微信、Webhook。

```textmate
cn.jowen.framework.extras.notification
├─ NotificationService           # send / sendAsync（CompletableFuture）/sendBatch
├─ NotificationRequest           # channel / to / subject / content / templateCode / templateParams / attachments / extra
├─ NotificationResult            # success / messageId / errorMessage / channel
├─ NotificationChannel           # EMAIL / SMS / DINGTALK / WECHAT_WORK / WEBHOOK / CUSTOM
├─ channel/                      # NotificationChannelHandler（SPI）
│  ├─ EmailNotificationHandler      # Jakarta Mail：HTML/附件/抄送密送/模板
│  ├─ SmsNotificationHandler        # 阿里云/腾讯云，SPI 扩展
│  ├─ DingTalkNotificationHandler   # 文本/Markdown/ActionCard/签名
│  ├─ WeChatWorkNotificationHandler # 文本/Markdown/卡片
│  └─ WebhookNotificationHandler    # POST/GET/自定义头/签名
├─ template/                     # NotificationTemplateEngine：SimpleTemplateEngine（$ { name }）/FreeMarkerTemplateEngine
└─ NotificationProperties        # enabled / email / sms / dingtalk / wechatWork / template
```

#### 4.6 excel/ — Excel 处理

##### 定位

基于 EasyExcel 的声明式导入导出，流式处理大数据量、模板填充、多 Sheet。

```textmate
cn.jowen.framework.extras.excel
├─ ExcelService               # export（HttpServletResponse/OutputStream）/exportToBytes/import / importAsync / fill（模板填充）
├─ ExcelExportRequest          # fileName / sheetName / data / head / sheets / writeHandler
├─ ExcelImportRequest          # inputStream / head / sheetNo / headerRowNumber / validator / batchSize
├─ ExcelImportResult<T>        # data / errors / totalCount / successCount / failCount
├─ ExcelError                  # rowIndex / columnIndex / fieldName / cellValue / errorMessage
├─ ExcelRowValidator<T>        # 行校验接口
├─ annotation/                 # @ExcelProperty（value/index/converter/format）/@ExcelIgnore / @ExcelMerge
├─ converter/                  # LocalDateConverter / LocalDateTimeConverter / BigDecimalConverter / EnumConverter / DictConverter
├─ handler/                    # ExcelWriteHandler（样式/列宽/条件格式）/ExcelReadListener（onHead/onData/onComplete）
└─ ExcelProperties             # enabled / defaultSheetName / defaultBatchSize（5000）/tempDir
```

#### 4.7 captcha/ — 验证码

##### 定位

图形/算术/滑块/短信验证码生成与校验，存储复用 framework-cache。

```textmate
cn.jowen.framework.extras.captcha
├─ CaptchaService              # generate(type) / verify(captchaId, code) / verify(...,deleteAfterVerify)
├─ CaptchaResult               # captchaId / image（Base64）/expiresIn / extra（滑块缺口位置）
├─ CaptchaType                 # IMAGE / ARITHMETIC / SLIDER / SMS
├─ generator/                  # CaptchaGenerator（SPI）/ImageCaptchaGenerator（字符+干扰线+噪点）/ArithmeticCaptchaGenerator / SliderCaptchaGenerator
├─ store/                      # CaptchaStore / CacheCaptchaStore（framework-cache）/LocalCaptchaStore（兜底）
└─ CaptchaProperties           # enabled / type / length（4）/width / height / ttl（5m）/caseSensitive / maxVerifyAttempts（5）
```

#### 4.8 ip2region/ — IP 地域解析

##### 定位

ip2region 离线库，纯内存查询（<0.1ms），无网络开销。

```textmate
cn.jowen.framework.extras.ip2region
├─ IpRegionService           # resolve(ip) / resolve(request)（自动处理 X-Forwarded-For 等）/ isInternalIp
├─ IpRegion                  # country / region / province / city / isp / fullRegion / ip
├─ IpRegionSearcher          # FileBuffer / IndexBuffer / VectorIndex 加载策略
└─ Ip2RegionProperties       # enabled / dbPath（classpath:ip2region.xdb）/loadType（FILE/MEMORY/INDEX）/trustedHeaders
```

#### 4.9 desensitize/ — 数据脱敏（core.desensitize 的 JSON 输出适配层）

##### 定位

**只做 JSON 序列化场景适配，不实现规则**。规则模型/执行器统一来自 `framework-core.desensitize`（冲突修正决议 #
1）；本子包负责把脱敏挂到 Jackson 3 输出链上。

```textmate
cn.jowen.framework.extras.desensitize
├─ annotation/              # 便捷注解（@PhoneDesensitize / @IdCardDesensitize ...）→ 解析为 core @DesensitizeField
├─ serializer/              # DesensitizeSerializerModifier（BeanSerializerModifier 扫描注解替换序列化器）/DesensitizeJsonSerializer / DesensitizeModule（Jackson 3Module）
├─ condition/               # @DesensitizeCondition（SpEL）/跳过上下文（基于 core ContextCarrier，管理员查看原始数据）
└─ DesensitizeProperties    # enabled / defaultReplacement（委托 core内核执行，配置前缀统一 framework.desensitize .*）
```

**与 core 的分工**：

- 内置策略（PHONE/ID_CARD/BANK_CARD/EMAIL/NAME/ADDRESS/PASSWORD/FIXED_PHONE/LICENSE_PLATE/CUSTOM）→
  `core.desensitize.DesensitizeStrategies`，本模块 **不再定义 `DesensitizeType`/`handler/`**；
- 自定义规则 → 实现 `core.desensitize.DesensitizeRule` 并经 SPI 注册，日志/结果集/JSON 输出一处定义、处处生效；
- 本模块只负责：注解解析（`@PhoneDesensitize` → `@DesensitizeField(PHONE)`）+ Jackson 3 `Module` 注册。

**Spring Boot 4.x 适配**：`DesensitizeModule` 基于 `tools.jackson`（Jackson 3）注册，`ObjectMapper` →
`JsonMapper.builder().build()`。

```textmate
public class UserVO {
    @PhoneDesensitize
    private String phone;        / / 输出: 138****1234
    @IdCardDesensitize
    private String idCard;       / / 输出: 110***********1234
}
// 临时跳过脱敏（管理员）——基于 core ContextCarrier
try(
var ignored = DesensitizeContext.skip()){...}
```

#### 4.10 operatelog/ — 操作日志

##### 定位

声明式操作日志，异步写入，模板化内容。

```textmate
cn.jowen.framework.extras.operatelog
├─ @OperateLog                # module / action/description（SpEL）/content（#oldObj/#newObj）/condition/async/extra
├─ OperateLogRecord           # traceId/module/action/description/content/operator/operatorId/requestMethod/requestUrl/requestParams（脱敏）/responseData/clientIp/clientLocation/browser/os/status/errorMessage/costTime/operateTime/extra
├─ OperateLogHandler          # 处理器接口（SPI）：handle(record)
├─ handler/                   # LoggingOperateLogHandler（默认）/DatabaseOperateLogHandler/MqOperateLogHandler
├─ OperateLogInterceptor      # 前置记录开始时间 → 后置解析 SpEL 组装记录 → 异常记录 → 异步写入（虚拟线程池）
├─ OperateLogContext          # setOperator/getOperator（基于 core ContextCarrier）
├─ OperateStatus              # SUCCESS/FAIL
└─ OperateLogProperties       # enabled/async/handler/includeRequestParams/includeResponseData/maxParamLength（2000）/excludeModules
```

**使用示例**：

```textmate

@OperateLog(module = "用户管理", action = "CREATE", description = "'新增用户: ' + # user.username")
public void createUser(User user) { ...}
```

#### 4.11 datapermission/ — 数据权限

##### 定位

基于 MyBatis 拦截器的行级数据权限（SQL 改写追加 WHERE）。

```textmate
cn.jowen.framework.extras.datapermission
├─ @DataPermission             # enabled / deptColumn（dept_id）/userColumn（create_by）/ignoreTables
├─ DataPermissionRule          # 接口：getExpression(TableInfo, mappedStatementId) ->Expression
├─ rule/                       # DeptDataPermissionRule（本人/本部门/本部门及子部门/全部）/UserDataPermissionRule / CustomDataPermissionRule
├─ DataPermissionInterceptor   # 拦截 SQL →解析注解 →取用户权限范围 →改写 SQL追加条件
├─ DataPermissionContext       # setCurrentUser / getCurrentUser / ignore / restore（基于 core ContextCarrier）
├─ UserInfo                    # userId / deptId / deptIds / dataScope
├─ DataScope                   # ALL / DEPT_AND_CHILD / DEPT / SELF / CUSTOM
└─ DataPermissionProperties    # enabled / defaultDeptColumn / defaultUserColumn / ignoreTables
```

**上下文统一（冲突修正决议 # 3）**：`DataPermissionContext` / `OperateLogContext` / 脱敏跳过上下文均读写
`core.context.ContextCarrier`（默认 ScopedValue，`framework.context.mode=threadlocal` 兼容切换），与 i18n / data-mybatis /
logger 共享同一套机制；跨虚拟线程迁移用 `ContextSnapshot`。

#### 4.12 config/ — 配置属性与工厂

##### 定位

**只保留 `@ConfigurationProperties` 与工厂类，不含任何 `*AutoConfiguration`**（冲突修正决议 # 2：AutoConfiguration 类全部上移
`framework-boot-autoconfigure`，此处不再定义、不再写 `AutoConfiguration.imports` 注册文件）。

```textmate
cn.jowen.framework.extras.config
├─ ExtrasProperties          # 总开关 framework.extras.enabled + 各功能 Properties 聚合
├─ LockProperties / RateLimitProperties / IdempotentProperties / StorageProperties
│   # 各功能配置绑定（@ConfigurationProperties 前缀 framework.extras .<feature>.*）
├─ NotificationProperties / ExcelProperties / CaptchaProperties / Ip2RegionProperties
├─ DesensitizeProperties     # 薄绑定：转发 framework.desensitize .*（core 统一前缀）
├─ OperateLogProperties / DataPermissionProperties
└─ *Factory                  # 各功能 Bean 工厂（被 boot-autoconfigure 引用装配）
```

**装配约定**：本模块暴露 `@ConfigurationProperties` 类与工厂方法；Bean 的实例化、条件判断、组合编排全部由
`framework-boot-autoconfigure` 的 `FrameworkExtrasAutoConfiguration` 完成（依赖本模块 `config` 的工厂）。

#### 4.13 support/ — 跨模块编排工具

##### 定位（冲突修正决议 # 8）

**不复制通用工具**。通用字符串/集合/反射/断言等一律用 `core.util`（唯一出处）；本子包只放 **跨模块编排** 需要的少量类型：
`ExtrasMarker`（装配标记接口）、`ExtrasContextBridge`（把 web 层用户信息写入 core ContextCarrier，供
operatelog/datapermission 消费）。

```textmate
cn.jowen.framework.extras.support
├─ ExtrasMarker           # 装配标记（boot-autoconfigure 条件判断用）
└─ ExtrasContextBridge    # Servlet 拦截器：登录用户 →ContextCarrier（operator/userInfo）
```

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────────────────────────────┐
│                      framework-extras                            │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              config（统一装配层）                           │  │
│  │  ExtrasAutoConfiguration ──→ ExtrasProperties             │  │
│  │  ├─ Lock / RateLimit / Idempotent / Captcha              │  │
│  │  │   └─ @ConditionalOnClass(CacheManager)                 │  │
│  │  ├─ Storage / Notification / Excel / Ip2Region           │  │
│  │  │   └─ @ConditionalOnClass(对应 SDK)                     │  │
│  │  ├─ Desensitize（@ConditionalOnClass(JsonMapper)）       │  │
│  │  └─ OperateLog / DataPermission / Support                 │  │
│  └────────────┬──────────────────────────────────────────────┘  │
│               │ 按功能依赖                                       │
│  ┌────────────▼──────────────────────────────────────────────┐  │
│  │  lock      │  ratelimit │  idempotent │  captcha          │  │
│  │  @Lockable │  @RateLimit│  @Idempotent│  CaptchaService   │  │
│  └────────────┬──────────────────────────────────────────────┘  │
│               ▼ 复用存储底座                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              framework-cache（CacheManager）              │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────┐ ┌────────────┐ ┌───────────┐ ┌──────────────┐  │
│  │  storage   │ │notification│ │   excel   │ │  ip2region   │  │
│  │  FileStor. │ │Notification│ │ ExcelSvc  │ │ IpRegionSvc  │  │
│  │  Local/    │ │ Email/SMS/ │ │ EasyExcel │ │ xdb 离线库   │  │
│  │  MinIO/OSS │ │ DingTalk/  │ │           │ │              │  │
│  │  / S3       │ │ WeCom/Web  │ │           │ │              │  │
│  └────────────┘ └────────────┘ └───────────┘ └──────────────┘  │
│                                                                  │
│  ┌────────────┐ ┌────────────┐ ┌──────────────┐               │
│  │desensitize│ │ operatelog │ │ datapermission│               │
│  │ @Phone... │ │ @OperateLog│ │ @DataPermis.  │               │
│  │ Jackson 3 │ │ 异步写入    │ │ MyBatis 拦截  │               │
│  └────────────┘ └────────────┘ └──────────────┘               │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  framework-core（SPI/Event/Exception）· Spring Boot 4.x   │  │
│  │  Jackson 3 · Micrometer 2.0 · 虚拟线程（ScopedValue）      │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0（零框架依赖）    support / annotation / 各功能独立接口
                      ▲
L1（按需依赖）       lock / ratelimit / idempotent / captcha
                    → 依赖 framework-cache（Redis 存储底座）

                    storage / notification / excel / ip2region
                    → 依赖各自第三方 SDK（全部 optional）

                    desensitize / operatelog / datapermission
                    → 零框架依赖，纯功能实现（operatelog 复用 core Event）
                      ▲
L2（依赖 L0+L1）     config（统一装配，唯一依赖 Spring Boot 的层）
```

**模块间规则**：

- 功能间零耦合：`lock` 不依赖 `ratelimit`，各自独立装配/独立开关；
- 全部第三方依赖 optional，`@ConditionalOnClass` 保证缺依赖时不装配、不报错；
- 仅 `config` 层依赖 Spring Boot 自动装配机制。

---

## 七、外部依赖

```xml

<dependencies>
    <!-- 框架内依赖 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-cache</artifactId>
        <optional>true</optional>   <!-- lock/ratelimit/idempotent/captcha -->
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

    <!-- ===== 以下为各功能可选依赖 ===== -->
    <!-- 文件存储 -->
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.aliyun.oss</groupId>
        <artifactId>aliyun-sdk-oss</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 消息通知 -->
    <dependency>
        <groupId>jakarta.mail</groupId>
        <artifactId>jakarta.mail-api</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Excel -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>easyexcel</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- IP 地域 -->
    <dependency>
        <groupId>org.lionsoul</groupId>
        <artifactId>ip2region</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 数据脱敏 → Jackson 3 -->
    <dependency>
        <groupId>tools.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- SpEL（注解 key/表达式） -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-expression</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Web（拦截器依赖 Servlet API） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
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
  extras:
    enabled: true                        # 总开关

    lock:
      enabled: true
      type: redis                        # local / redis
      key-prefix: "lock:"
      default-lease-time: 30s
      watchdog-enabled: true

    ratelimit:
      enabled: true
      default-algorithm: sliding-window
      key-prefix: "ratelimit:"

    idempotent:
      enabled: true
      default-ttl: 60s
      key-prefix: "idempotent:"
      token-header: X-Idempotent-Token

    storage:
      enabled: true
      type: minio                        # local / minio / aliyun-oss / aws-s3
      minio:
        endpoint: http://127.0.0.1:9000
        access-key: minioadmin
        secret-key: minioadmin
        bucket: myapp
      object-name-strategy: date-path

    notification:
      enabled: true
      email:
        host: smtp.example.com
        port: 465
        username: noreply@example.com
        password: xxx
        from: noreply@example.com
      dingtalk:
        webhook-url: https://oapi.dingtalk.com/robot/send?access_token=xxx

    excel:
      enabled: true
      default-batch-size: 5000

    captcha:
      enabled: true
      type: arithmetic
      length: 4
      ttl: 5m

    ip2region:
      enabled: true
      load-type: memory

    desensitize:
      enabled: true              # 转发 framework.desensitize.enabled（core 统一前缀）
      default-replacement: "*"   # 转发 framework.desensitize.default-replacement

    operatelog:
      enabled: true
      async: true
      handler: database

    datapermission:
      enabled: true
      default-dept-column: dept_id
      default-user-column: create_by
```

---

## 九、使用方式

#### 9.1 引入依赖

```xml

<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-extras</artifactId>
</dependency>
        <!-- 按功能补充对应 SDK（可选）：minio / easyexcel / ip2region ... -->
```

#### 9.2 功能启用条件汇总

| 功能        | 启用条件                                                         | 核心依赖               |
|:------------|:-----------------------------------------------------------------|:-----------------------|
| 分布式锁    | `framework.extras.lock.enabled=true` + classpath 有 CacheManager | framework-cache        |
| 接口限流    | `framework.extras.ratelimit.enabled=true` + CacheManager         | framework-cache        |
| 幂等控制    | `framework.extras.idempotent.enabled=true` + CacheManager        | framework-cache        |
| 文件存储    | `framework.extras.storage.enabled=true`                          | 对应 SDK               |
| 消息通知    | `framework.extras.notification.enabled=true`                     | Jakarta Mail（邮件时） |
| Excel 处理  | `framework.extras.excel.enabled=true` + EasyExcel                | EasyExcel              |
| 验证码      | `framework.extras.captcha.enabled=true`                          | framework-cache        |
| IP 地域解析 | `framework.extras.ip2region.enabled=true` + ip2region            | ip2region              |
| 数据脱敏    | `framework.extras.desensitize.enabled=true` + Jackson 3          | jackson-databind       |
| 操作日志    | `framework.extras.operatelog.enabled=true`                       | —                      |
| 数据权限    | `framework.extras.datapermission.enabled=true`                   | —                      |

#### 9.3 典型组合示例

```textmate
// 限流 + 幂等 + 操作日志 + 锁 组合使用
@RateLimit(key = "'api:order:create'", permits = 10, period = 60000, scope = USER)
@Idempotent(mode = TOKEN, ttl = 120000)
@OperateLog(module = "订单", action = "CREATE", description = "'下单: ' + # request.orderNo")
@Lockable(key = "'order:' + # request.orderNo", waitTime = 5000)
public Order createOrder(OrderCreateRequest request) { ...}
```

---

## 十、SPI 扩展点汇总

| 功能     | 扩展点接口                                | 用途                                   |
|:---------|:------------------------------------------|:---------------------------------------|
| 分布式锁 | `DistributedLock`                         | 自定义锁实现                           |
| 限流     | `RateLimiter`                             | 自定义限流算法                         |
| 幂等     | `IdempotentValidator`                     | 自定义幂等校验                         |
| 文件存储 | `FileStorage` / `ObjectNameStrategy`      | 自定义存储后端 / 命名策略              |
| 消息通知 | `NotificationChannelHandler`              | 自定义通知渠道                         |
| Excel    | `ExcelRowValidator` / `ExcelWriteHandler` | 自定义行校验 / 写入样式                |
| 验证码   | `CaptchaGenerator` / `CaptchaStore`       | 自定义生成 / 存储                      |
| 数据脱敏 | `DesensitizeRule`（core.desensitize）     | 自定义脱敏规则（JSON/日志/结果集共用） |
| 操作日志 | `OperateLogHandler`                       | 自定义日志持久化                       |
| 数据权限 | `DataPermissionRule`                      | 自定义权限规则                         |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│                    业务应用（Application）                    │
│  组合使用 @Lockable / @RateLimit / @Idempotent / @OperateLog │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-boot（适配编排层）                     │
│  ExtrasAutoConfiguration（framework.extras.enabled）        │
│  ├─ 按功能开关注册 Bean 与 AOP 拦截器                       │
│  └─ 各功能 Properties 绑定                                  │
└──────────────────────────┬──────────────────────────────────┘
                           │ 按需
┌──────────────────────────▼──────────────────────────────────┐
│              framework-extras（工具集）                       │
│  lock / ratelimit / idempotent / storage / notification     │
│  excel / captcha / ip2region / desensitize / operatelog     │
│  datapermission                                             │
└───────┬──────────────────────────────┬──────────────────────┘
        │ 复用存储底座                  │ 可选依赖
┌───────▼───────────────┐  ┌───────────▼─────────────────────┐
│  framework-cache      │  │  MinIO/OSS/S3/EasyExcel/ICU/    │
│  （锁/限流/幂等/验证码）│  │  Jakarta Mail/ip2region（optional）│
└───────────────────────┘  └─────────────────────────────────┘
```

**依赖方向**：业务 → extras（注解/API）→ cache/core（底座）+ 按需第三方 SDK。extras
是框架的"业务武器库"，与数据访问、缓存、国际化正交，独立演进、独立开关。
