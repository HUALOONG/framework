# framework-extras 模块架构设计

> 文档元信息
> - **模块**：framework-extras
> - **关键词**：扩展工具集、聚合父模块、common/message/storage/web 四域、按需装配
> - **描述**：框架扩展工具集聚合父模块（纯 POM，零代码），下挂 common / message / storage / web 四个子模块，承载分布式锁、限流、幂等、文件存储、消息通知、验证码、数据权限、操作日志等场景化能力
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-extras` 是框架的 **扩展工具集聚合父模块（纯 POM，零代码）**，与 `framework-data` / `framework-boot` 同属聚合父模块，下辖 common / message / storage / web 四个子模块，承载分布式锁、限流、幂等、文件存储、消息通知、验证码、数据权限、操作日志等场景化能力。

```text
framework-extras（聚合父模块，纯 POM）
├─ framework-extras-common     # 公共基础：配置 Properties + 异常类（L0）
├─ framework-extras-message    # 消息通知：邮件/短信/钉钉/企微/Webhook
├─ framework-extras-storage    # 文件存储：本地/MinIO/阿里云OSS/AWS S3
└─ framework-extras-web        # Web 工具集：锁/限流/幂等/验证码/数据权限/操作日志
```

**核心职责**：

| 子模块                     | 职责                                                                                |
| :------------------------- | :---------------------------------------------------------------------------------- |
| framework-extras（聚合父） | 统一声明四个子模块、收敛构建与版本，自身零代码、不注册任何 Bean                     |
| framework-extras-common    | 提供 extras 内部共享的配置 Properties 与异常类，作为各域公共底座（L0，仅依赖 core） |
| framework-extras-message   | 统一消息通知抽象，屏蔽邮件/短信/钉钉/企微/Webhook 渠道差异，按条件按需装配          |
| framework-extras-storage   | 统一文件存储抽象，支持本地/MinIO/阿里云 OSS/AWS S3 多后端，按条件按需装配           |
| framework-extras-web       | 提供分布式锁、接口限流、幂等控制、验证码、数据权限、操作日志六项 Web 层工具能力     |

**与相邻模块的边界**：

| 模块                           | 分工                                                    |
| :----------------------------- | :------------------------------------------------------ |
| `framework-core`               | 基础设施（SPI/异常/断言/上下文），extras 各域的公共底座 |
| `framework-cache`              | 缓存/锁底座，web 域的 lock/idempotent/captcha 可复用    |
| `framework-extras-common`      | extras 内部共享的 Properties + Exception，L0 零内部依赖 |
| `framework-boot-autoconfigure` | 承载 extras 各域的自动装配（ExtrasAutoConfiguration）   |

> 与 `framework-data` / `framework-boot` 一致，本聚合器 **零代码**，不触发任何 Bean 注册；运行时行为由四个子模块 + autoconfigure 承载。

**核心价值**：

| 维度     | 说明                                                                        |
| :------- | :-------------------------------------------------------------------------- |
| 依赖隔离 | 各域独立依赖、optional 收紧，下游按需引入单域，避免引入无关能力的传递依赖   |
| 公共复用 | common 统一收口各能力 Properties 与 Exception，消除配置/异常散落重复        |
| 按需装配 | 各域按 `@ConditionalOnClass` 激活，可选三方 SDK 全部 optional，不绑架下游   |
| 职责清晰 | 四域边界明确、文档与测试各自独立，便于维护与扩展                            |
| 构建灵活 | 可整体构建 extras 域，也可单独构建某一能力子域（如 `framework-extras-web`） |



---

## 二、功能清单与能力归属

| 子模块                   | 功能                        | 形式           | 依赖                                              | 说明                                                   |
| :----------------------- | :-------------------------- | :------------- | :------------------------------------------------ | :----------------------------------------------------- |
| framework-extras-common  | 配置属性                    | 纯 POJO        | framework-core                                    | 各能力 `@ConfigurationProperties`（12 项能力聚合开关） |
| framework-extras-common  | 异常类                      | 纯类           | framework-core                                    | 各能力 `ExtrasException` 派生体系                      |
| framework-extras-message | 消息通知（原 notification） | 服务 + 渠道    | common, core / 可选：angus-mail, okhttp, 短信 SDK | 邮件/短信/钉钉/企微/Webhook，统一 `messageService`     |
| framework-extras-storage | 文件存储                    | 服务 + 多后端  | common, core / 可选：minio, aliyun-oss, aws-s3    | 本地/MinIO/阿里云 OSS/AWS S3，统一 `FileStorage`       |
| framework-extras-web     | 分布式锁                    | AOP + 注解     | common, core, cache(可选), aop                    | `@Lockable`，Redis/本地实现                            |
| framework-extras-web     | 接口限流                    | AOP + 注解     | common, core, aop                                 | `@RateLimit`，4 种算法                                 |
| framework-extras-web     | 幂等控制                    | AOP + 注解     | common, core, cache(可选), aop                    | `@Idempotent`，Token/Key 双模式                        |
| framework-extras-web     | 验证码                      | 服务 + 生成器  | common, core, cache(可选)                         | 图形/算术/滑块/短信                                    |
| framework-extras-web     | 数据权限                    | MyBatis 拦截器 | common, core                                      | `@DataPermission`，行级规则改写                        |
| framework-extras-web     | 操作日志                    | AOP + 注解     | common, core, aop                                 | `@OperateLog`，异步写入                                |
| （待规划）               | Excel 处理                  | —              | —                                                 | 原 12 项之一，新四域未显式承接                         |
| （待规划）               | IP 地域解析                 | —              | —                                                 | 原 12 项之一，新四域未显式承接                         |
| （待规划）               | 数据脱敏                    | —              | —                                                 | 原 12 项之一，core 已含脱敏内核（Desensitizer）        |


---

## 三、整体包结构

```text
framework-extras
├─ pom.xml                              # 聚合父模块（仅声明四个子模块）
├─ framework-extras-common/
│  └─ src/main/java/cn/jowen/framework/extras/common/
│     ├─ config/        # 各功能 Properties POJO（纯 POJO）
│     └─ exception/     # 各功能异常类
├─ framework-extras-message/
│  └─ src/main/java/cn/jowen/framework/extras/message/
│     ├─ channel/       # Email / Sms / DingTalk / WeChatWork / Webhook / Console
│     ├─ template/      # 模板引擎（预留 SPI）
│     └─ config/        # 各渠道 Properties
├─ framework-extras-storage/
│  └─ src/main/java/cn/jowen/framework/extras/storage/
│     ├─ impl/          # Local / Minio / AliyunOss / AwsS3
│     ├─ strategy/      # 命名策略（日期/UUID/Hash/原始名）
│     ├─ registry/      # 多后端管理
│     └─ config/        # 各后端 Properties
└─ framework-extras-web/
   └─ src/main/java/cn/jowen/framework/extras/web/
      ├─ lock/          # 分布式锁 @Lockable
      ├─ ratelimit/     # 接口限流 @RateLimit
      ├─ idempotent/    # 幂等控制 @Idempotent
      ├─ captcha/       # 验证码
      ├─ datapermission/# 数据权限 @DataPermission
      ├─ operatelog/    # 操作日志 @OperateLog
      ├─ config/        # 各功能 Properties
      └─ exception/     # 异常类
```

---

## 四、各子模块详细设计

### 4.1 framework-extras-common（公共基础）

**定位**：extras 公共枢纽，承载跨子模块共享的配置属性（Properties POJO）与异常类，依赖 `framework-core`，零内部依赖（L0）。

- `config/`：`ExtrasProperties`（总开关）+ `LockProperties` / `RateLimitProperties` / `IdempotentProperties` / `StorageProperties` / `NotificationProperties` / `ExcelProperties` / `CaptchaProperties` / `Ip2RegionProperties` / `DesensitizeProperties` / `OperateLogProperties` / `DataPermissionProperties`（12 项能力聚合开关）
- `exception/`：`ExtrasException`（基类）+ `LockException` / `RateLimitException` / `IdempotencyException` / `StorageException` / `NotificationException` / `CaptchaException` / `DataPermissionException`

```xml
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-core</artifactId>
</dependency>
```

### 4.2 framework-extras-message（消息通知）

**定位**：统一消息通知抽象，屏蔽邮件/短信/钉钉/企微/Webhook 渠道差异，按 `@ConditionalOnClass` 按需装配。依赖 `common` + `core`。

- 核心：`messageService`（send/sendAsync/sendBatch）、`messageRequest`、`messageResult`、`messageChannel`（枚举）、`messageChannelHandler`（SPI）
- 渠道：`EmailmessageHandler` / `SmsmessageHandler` / `DingTalkmessageHandler` / `WeChatWorkmessageHandler` / `WebhookmessageHandler` / `ConsolemessageHandler`
- 模板：`messageTemplateEngine`（SPI，预留 SimpleTemplateEngine 骨架）
- 可选依赖（全部 optional）：`angus-mail` / `okhttp` / `阿里云短信 SDK`

```java
messageResult result = messageService.send(
    messageRequest.builder()
        .channel(messageChannel.EMAIL)
        .to("user@example.com")
        .subject("测试邮件")
        .content("<h1>你好</h1>")
        .build());
```

### 4.3 framework-extras-storage（文件存储）

**定位**：统一文件存储抽象层，支持本地/MinIO/阿里云OSS/AWS S3 四种后端，按 `@ConditionalOnClass` 按需装配。依赖 `common` + `core`。

- 核心：`FileStorage`（upload/download/getPresignedUrl/delete/exists/listObjects）、`FileInfo`、`FileStorageManager`、`StorageConfig`（注解）
- 实现：`LocalFileStorage` / `MinioFileStorage` / `AliyunOssFileStorage` / `AwsS3FileStorage`
- 策略：`ObjectNameStrategy` 接口 + `DatePath`/`Uuid`/`Hash`/`OriginalName` 四种，工厂 `ObjectNameStrategyFactory`
- 可选依赖（全部 optional）：`minio` / `aliyun-sdk-oss` / `aws s3 sdk v2`

```java
FileInfo info = storageManager.getStorage().upload(inputStream, "photo.jpg");
String url = storageManager.getStorage().getPresignedUrl(info.getObjectName(), Duration.ofHours(1));
```

### 4.4 framework-extras-web（Web 工具集）

**定位**：框架 Web 层工具集，提供 6 项高频能力：分布式锁、接口限流、幂等控制、验证码、数据权限、操作日志。通过 AOP 声明式使用，lock/idempotent/captcha 可复用 `framework-cache` 作为存储底座。依赖 `common` + `core`，可选 `cache` + `spring-aop` + `aspectj` + `spring-expression`。

| 子包           | 核心注解/接口                            | 关键实现                                        |
| :------------- | :--------------------------------------- | :---------------------------------------------- |
| lock           | `@Lockable` / `DistributedLock`          | `RedisDistributedLock`（Lettuce+Lua）/ 本地兜底 |
| ratelimit      | `@RateLimit` / `RateLimiter`             | 固定窗口/滑动窗口/漏桶/令牌桶                   |
| idempotent     | `@Idempotent` / `IdempotentValidator`    | `RedisIdempotentValidator` / 本地实现           |
| captcha        | `CaptchaService`                         | 图形/算术/滑块/短信 生成器 + 缓存存储           |
| datapermission | `@DataPermission` / `DataPermissionRule` | MyBatis 拦截器改写 SQL（部门/个人/自定义）      |
| operatelog     | `@OperateLog` / `OperateLogHandler`      | 日志/数据库/MQ 处理器，异步分发                 |

```java
@RateLimit(key = "'api:order:create'", permits = 10, period = 60000, scope = USER)
@Idempotent(mode = IdempotentMode.TOKEN, ttl = 120000)
@OperateLog(module = "订单", action = "CREATE", description = "'下单: ' + #request.orderNo")
@Lockable(key = "'order:' + #request.orderNo", waitTime = 5000)
public Order createOrder(OrderCreateRequest request) { ... }
```

> 各子模块完整设计（包结构、依赖矩阵、配置属性、SPI 扩展点）详见各自子目录的 `README.md`：
> - [framework-extras-common/README.md](framework-extras-common/README.md)
> - [framework-extras-message/README.md](framework-extras-message/README.md)
> - [framework-extras-storage/README.md](framework-extras-storage/README.md)
> - [framework-extras-web/README.md](framework-extras-web/README.md)

---

## 五、核心类关系图

```text
┌───────────────────────────────────────────────────────────────────────┐
│              framework-extras（纯 POM 聚合父模块）                    │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────┐          │
│  │     framework-extras-common（L0 公共基础）              │          │
│  │   config/*Properties（12 项能力） + exception/*         │          │
│  │   依赖：framework-core                                  │          │
│  └────────────────────────────┬────────────────────────────┘          │
│                               │ 被依赖                                │
│  ┌──────────────┬─────────────▼──┬─────────────────────────────────┐  │
│  │ message      │ storage        │ web                             │  │
│  │ (通知)       │ (文件存储)     │ (锁/限流/幂等/验证码/权限/日志) │  │
│  └──────┬───────┴───────────┬────┴─────────┬───────────────────────┘  │
│         │ 可选：angus-mail  │ 可选：minio  │ 可选：cache           │  │
│         │       okhttp      │       oss    │ (lock/idem/captcha)   │  │
│         │       短信SDK     │       s3     │                       │  │
│         └───────────────────┴──────────────┴───────────────────────┘  │
│                                                                       │
│  装配承载：framework-boot-autoconfigure（ExtrasAutoConfiguration）    │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0 (零内部依赖)        framework-extras-common
                          ▲
L1 (各能力子模块)      framework-extras-storage
                       framework-extras-message
                       framework-extras-web
                          ▲
构建聚合               ★ framework-extras（本模块，纯 POM）
```

**模块间规则**：

1. **common 为 L0**：所有能力子模块均依赖 `framework-extras-common` 取得 Properties 与 Exception，common 自身仅依赖 `framework-core`；
2. **能力域互不依赖**：message / storage / web 之间无横向依赖，各自独立发布、按需引入；
3. **可选依赖全部 optional**：各域的三方 SDK（邮件/存储后端/cache/AOP）声明为 `<optional>true</optional>`，按需触发装配，不强制绑架下游；
4. **聚合器零代码**：`framework-extras` 仅声明 `<modules>` 四个子模块，不注册任何 Bean；
5. **装配外置**：各域 `@AutoConfiguration` 经 `framework-boot-autoconfigure` 的 `ExtrasAutoConfiguration` 统一接入。

---

## 七、外部依赖（聚合视角）

| 依赖                       | scope      | 用途                           |
| :------------------------- | :--------- | :----------------------------- |
| `framework-extras-common`  | compile    | 配置/异常公共底座              |
| `framework-extras-message` | compile    | 消息通知能力                   |
| `framework-extras-storage` | compile    | 文件存储能力                   |
| `framework-extras-web`     | compile    | Web 工具集能力                 |
| `framework-core`           | 传递       | 基础设施                       |
| `framework-cache`          | 传递(可选) | web 域 lock/idempotent/captcha |

> 本聚合器自身零外部依赖；版本由根 pom 与 `framework-bom` 裁决。各域三方可选依赖（angus-mail / okhttp / minio / oss / aws-s3 / cache / aop）均为 optional，详见各子模块 README §七。

---

## 八、配置属性（聚合示例）

各能力配置统一收敛在 `framework.extras.*` 前缀，总开关与各域开关如下（详细逐字段见各子模块 README §八）：

```yaml
framework:
  extras:
    enabled: true                       # 总开关
    # —— common 聚合的 12 项能力开关 ——
    lock:        { enabled: true }
    ratelimit:   { enabled: true }
    idempotent:  { enabled: true }
    storage:     { enabled: true, type: local }
    message:     { enabled: true }
    excel:       { enabled: true }
    captcha:     { enabled: true, type: arithmetic }
    ip2region:   { enabled: true }
    desensitize: { enabled: true }
    operatelog:  { enabled: true, async: true }
    datapermission: { enabled: true }
```

构建期用法：

```bash
# 构建整个 extras 域（连带 common）
mvn install -pl framework-extras -am

# 仅构建单个能力域（如只构建 web）
mvn install -pl framework-extras-web -am
```

---

## 九、使用方式

#### 9.1 维护者：构建 extras 域

```bash
mvn install -pl framework-extras -am    # 构建 common/message/storage/web 及上游
```

#### 9.2 下游用户（按域引入）

```xml
<!-- 只需要文件存储能力 -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-extras-storage</artifactId>
</dependency>

<!-- 只需要 Web 工具集 -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-extras-web</artifactId>
</dependency>

<!-- 只需要消息通知 -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-extras-message</artifactId>
</dependency>
```

> 与 `framework-data` / `framework-boot` 一致，下游运行时通常不依赖聚合器 `framework-extras` 本身，而是按需引入具体子模块；版本由 `framework-bom` 管理。

---

## 十、SPI 扩展点汇总

| 扩展点接口              | 所在子模块 | 所在包                | 用途                   |
| :---------------------- | :--------- | :-------------------- | :--------------------- |
| `messageChannelHandler` | message    | message               | 自定义通知渠道         |
| `messageTemplateEngine` | message    | message/template      | 自定义模板引擎（预留） |
| `FileStorage`           | storage    | storage               | 自定义存储后端         |
| `ObjectNameStrategy`    | storage    | storage/strategy      | 自定义对象命名策略     |
| `DistributedLock`       | web        | web/lock              | 自定义锁实现           |
| `RateLimiter`           | web        | web/ratelimit         | 自定义限流算法         |
| `IdempotentValidator`   | web        | web/idempotent        | 自定义幂等校验         |
| `CaptchaGenerator`      | web        | web/captcha/generator | 自定义验证码生成器     |
| `CaptchaStore`          | web        | web/captcha/store     | 自定义验证码存储       |
| `DataPermissionRule`    | web        | web/datapermission    | 自定义数据权限规则     |
| `OperateLogHandler`     | web        | web/operatelog        | 自定义操作日志处理器   |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│  根 pom（framework-parent）—— 版本字典 + 全仓库 aggregator  │
└───────┬──────────────────────────────┬──────────────────────┘
        │ 聚合                         │ 版本
┌───────▼───────────────────┐  ┌───────▼───────────────────┐
│ framework-data（数据域）  │  │ framework-extras（扩展域）│
│ core/jdbc/mybatis         │  │ common/message/           │
│                           │  │ storage/web               │
└───────────────────────────┘  └────────────┬──────────────┘
                                            │ 依赖
                              ┌─────────────▼──────────────┐
                              │ framework-core（基础设施） │
                              │ framework-cache（可选底座）│
                              └────────────────────────────┘

装配承载：framework-boot-autoconfigure（ExtrasAutoConfiguration 统一接入四域）
```

**一句话总结**：`framework-extras` 是 extras 扩展域的 **聚合父模块**（与 `framework-data` / `framework-boot` 对称，纯 POM、构建/维护视角）；`framework-extras-common` 是四域共享的 L0 公共底座，`message` / `storage` / `web` 为三个能力子域；运行时行为由子模块 + `framework-boot-autoconfigure` 承载，版本由 `framework-bom` 管理。
