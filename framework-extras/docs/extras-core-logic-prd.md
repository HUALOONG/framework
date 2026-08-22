# framework-extras 本轮范围裁剪 PRD（核心逻辑层 / 简单 PRD）

> 文档元信息
> - **模块**：framework-extras
> - **本轮主题**：核心逻辑层优先 — 零/低外部依赖、可独立单测的「接口 + 本地实现 + 算法」
> - **基线**：Spring Boot 4.x + Java 21（Jackson 3、虚拟线程）
> - **文档角色**：README 已是完整设计（充当完整 PRD），本文档为**本轮子集裁剪**，明确边界、类清单、验收标准
> - **包约定**：`cn.jowen.framework.extras.<feature>`（已与既有 `lock`/`ratelimit`/`idempotent` 实现对齐）

---

## 一、产品目标

**本轮交付 `framework-extras` 的 6 项零/低外部依赖核心能力的纯本地实现层**（captcha、storage、operatelog、datapermission、notification、desensitize）：只落地「接口 + 本地/算法实现 + 模型/枚举/注解」，可独立单测；暂不实现 AOP 拦截器、Redis 分布式实现与自动装配上移，形成可直接被后续装配层复用的"逻辑内核"。

---

## 二、依赖与构建约束（决策 2 落地）

| 依赖 | 范围 | 用途 | 本轮是否必须 |
|:-----|:-----|:-----|:-------------|
| `framework-core` | compile | 提供 `core.context.ContextCarrier`/`ContextKey`、`core.event`、`core.desensitize`、`core.spi`、`core.util`、`core.exception` | 是（已存在） |
| `framework-cache` | compile | 仅既有 lock/ratelimit/idempotent 使用；**本轮 6 项不依赖** | 否 |
| `tools.jackson.core:jackson-databind` | **optional** | desensitize 的 Jackson 3 序列化适配（`DesensitizeModule` 等） | 仅 desensitize 需要，缺依赖时该子包条件跳过 |
| `org.jspecify:jspecify` | compile | 空安全注解（`@NullMarked`/`@Nullable`），core 已传递 | 是（传递） |
| JUnit 5 / AssertJ | test | 独立单测 | 是 |

**说明**：
- 本轮 6 项均不引入任何第三方 SDK（MinIO/OSS/S3/EasyExcel/ip2region/jakarta.mail 等）依赖。
- `jackson-databind` 以 `optional` 加入 pom，由 BOM 管理版本（**不写 version**）；缺失时 desensitize 子包不装配、不报错。
- 所有功能保持「可独立 `new` 出来跑单测」的能力，不依赖 Spring 容器启动。

---

## 三、Out of Scope（本轮明确排除）

> 以下能力已规划，但**本轮不实现**，列为「已规划待后续」，PRD 中单列以示边界。

- 所有 `@注解` 的 AOP 拦截器：`LockInterceptor` / `RateLimitInterceptor` / `IdempotentInterceptor` / `OperateLogInterceptor` / `DataPermissionInterceptor`
- Redis 分布式实现：`RedisDistributedLock` / `RedisIdempotentValidator` / `RedisCaptchaStore` / `CacheCaptchaStore` 等
- 自动装配上移 `framework-boot-autoconfigure`（`ExtrasAutoConfiguration` 及其 `@ConfigurationProperties` 上移）
- storage 云后端：MinIO / 阿里云 OSS / AWS S3；`@StorageConfig` 注解
- notification 真实渠道：`EmailNotificationHandler` / `SmsNotificationHandler` / `DingTalkNotificationHandler` / `WeChatWorkNotificationHandler` / `WebhookNotificationHandler`
- excel（需 EasyExcel）、ip2region（需库 + 数据文件）
- config 完整装配层（本轮只在各功能内放**轻量 Properties POJO**，不写 `AutoConfiguration`）
- storage/notification/operatelog 的真实持久化 Handler（`DatabaseOperateLogHandler` / `MqOperateLogHandler` 等）

---

## 四、需求池（P0 / P1 / P2）

> 优先级口径：P0 = 本轮必须交付；P1 = 应有但可延后；P2 = 锦上添花/后续。

| 编号 | 需求 | 优先级 | 归属 |
|:-----|:-----|:-------|:-----|
| R-captcha-1 | 验证码生成（图形/算术）→ Base64 PNG + 内存存储 | P0 | captcha |
| R-captcha-2 | 验证码校验（比对 + 可选删除 + TTL 清理） | P0 | captcha |
| R-storage-1 | 本地磁盘文件存储（upload/download/exists/delete/list） | P0 | storage |
| R-storage-2 | 4 种对象命名策略（日期/哈希/UUID/原名） | P0 | storage |
| R-operatelog-1 | 操作日志模型 + Handler 接口 + 默认日志 Handler | P0 | operatelog |
| R-operatelog-2 | 异步分发器（虚拟线程/ExecutorService） | P0 | operatelog |
| R-operatelog-3 | `@OperateLog` 注解定义（零依赖，仅声明） | P0 | operatelog |
| R-datapermission-1 | 规则引擎：UserInfo+DataScope → WHERE 片段 | P0 | datapermission |
| R-datapermission-2 | 部门/用户/自定义三类规则实现 | P0 | datapermission |
| R-datapermission-3 | 基于 `core.context.ContextCarrier` 的上下文读写 | P0 | datapermission |
| R-notification-1 | 通知模型 + Service 接口 + Channel SPI | P0 | notification |
| R-notification-2 | 本地调试 Handler（Console / Log） | P0 | notification |
| R-desensitize-1 | 便捷脱敏注解（Phone/IdCard/BankCard/Email/Name/Address…）→ 解析为 core `@DesensitizeField` | P0 | desensitize |
| R-desensitize-2 | Jackson 3 序列化适配（`DesensitizeModule` / `SerializerModifier` / `JsonSerializer`） | P0 | desensitize |
| R-pom-1 | pom 追加 `jackson-databind`（optional，BOM 管理） | P0 | 构建 |
| R-config-1 | 各功能轻量 Properties POJO（非 AutoConfiguration） | P1 | 各功能 |
| R-operatelog-4 | 可选复用 `core.event.EventBus` 派发日志事件 | P2 | operatelog（见待确认） |
| —— | 其余 README 12 项未落地内容 | 已规划待后续 | Out of Scope |

---

## 五、各子包详细 PRD

### 5.1 captcha（验证码）— 零依赖（JDK 图形）

**定位**：基于 JDK `BufferedImage` 生成图形/算术验证码，内存存储 + TTL，可独立单测。SLIDER 缺口图、SMS 真实发送、RedisStore 本轮不做。

#### 用户故事
```java
// 生成图形验证码
CaptchaResult r = captchaService.generate(CaptchaType.IMAGE);
// r.getImageBase64() -> "data:image/png;base64,xxxx" 非空
// r.getCaptchaId() 用于后续校验

// 校验（正确码返回 true；错误码返回 false；可选校验后删除）
boolean ok = captchaService.verify(r.getCaptchaId(), "AB3X", true);
```

#### 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.captcha` | 接口 | `CaptchaService` | `generate(type)` / `verify(id, code)` / `verify(id, code, deleteAfter)` |
| `cn.jowen.framework.extras.captcha` | 模型 | `CaptchaResult` | captchaId / imageBase64 / expiresIn / extra |
| `cn.jowen.framework.extras.captcha` | 枚举 | `CaptchaType` | IMAGE / ARITHMETIC / SLIDER / SMS |
| `cn.jowen.framework.extras.captcha.generator` | 接口 | `CaptchaGenerator` | 生成 `BufferedImage` + 答案 |
| `cn.jowen.framework.extras.captcha.generator` | 实现 | `ImageCaptchaGenerator` | 字符 + 干扰线 + 噪点 → Base64 PNG（取答案返回） |
| `cn.jowen.framework.extras.captcha.generator` | 实现 | `ArithmeticCaptchaGenerator` | 随机算式（如 `3+7=?`）→ 答案 |
| `cn.jowen.framework.extras.captcha.store` | 接口 | `CaptchaStore` | `put` / `get` / `remove` / `clearExpired` |
| `cn.jowen.framework.extras.captcha.store` | 实现 | `LocalCaptchaStore` | 内存 `Map` + TTL 定时/惰性清理 |
| `cn.jowen.framework.extras.captcha` | 实现 | `LocalCaptchaService` | 组合：generate→存 store→返回；verify→查 store 比对→可选删除 |

#### 验收标准
- [ ] `generate(IMAGE)` 返回非空 `captchaId` 与非空 `imageBase64`（前缀 `data:image/png;base64,`）。
- [ ] `generate(ARITHMETIC)` 答案与算式自洽（可解析出正确数字）。
- [ ] 正确码 `verify` 返回 `true`；错误码返回 `false`。
- [ ] `verify(..., deleteAfter=true)` 后再次校验同一 `captchaId` 返回 `false`（已删除）。
- [ ] 超过 `expiresIn` 的条目在 `clearExpired` 后被判定失效。
- [ ] 单测无需 Spring，直接 `new LocalCaptchaService(new LocalCaptchaStore())`。

---

### 5.2 storage（文件存储）— 零依赖（本地磁盘）

**定位**：统一文件存储抽象，本轮仅 `LocalFileStorage`（本地磁盘 + 日期分目录），基于 `java.nio.file`。云后端、`@StorageConfig` 不做。

#### 用户故事
```java
FileStorage storage = storageManager.getStorage();        // 默认本地
FileInfo info = storage.upload(inputStream, "avatar.png"); // 落盘 + 返回 objectName/size/url
boolean exists = storage.exists(info.getObjectName());     // true
InputStream in = storage.download(info.getObjectName());
```

#### 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.storage` | 接口 | `FileStorage` | upload / download / getPresignedUrl / delete / deleteBatch / exists / getFileInfo / listObjects |
| `cn.jowen.framework.extras.storage` | 模型 | `FileInfo` | objectName / originalFilename / contentType / size / etag / lastModified / url / metadata |
| `cn.jowen.framework.extras.storage` | 接口/管理器 | `FileStorageManager` | `getStorage()` / `getStorage(name)` / `registerStorage(name, storage)` |
| `cn.jowen.framework.extras.storage.strategy` | 接口 | `ObjectNameStrategy` | `generate(originalFilename)` → objectName |
| `cn.jowen.framework.extras.storage.strategy` | 实现 | `DatePathStrategy` | `yyyy/MM/dd/<uuid>_<原名>` |
| `cn.jowen.framework.extras.storage.strategy` | 实现 | `HashStrategy` | 内容哈希 / 分桶命名 |
| `cn.jowen.framework.extras.storage.strategy` | 实现 | `UuidStrategy` | UUID 命名 |
| `cn.jowen.framework.extras.storage.strategy` | 实现 | `OriginalNameStrategy` | 保留原始文件名 |
| `cn.jowen.framework.extras.storage.impl` | 实现 | `LocalFileStorage` | 基于 `java.nio.file` 的本地磁盘 CRUD + 日期分目录 |

#### 验收标准
- [ ] `upload` 后 `exists(objectName)` 为 `true`，`getFileInfo` 返回正确 `size`/`contentType`。
- [ ] `download` 得到的流内容与上传一致（字节比对）。
- [ ] `delete` 后 `exists` 为 `false`；`deleteBatch` 批量移除。
- [ ] 命名策略生效：同文件经 `DatePathStrategy` 落盘路径含日期目录。
- [ ] `listObjects` 返回目录下对象集合（可按前缀过滤）。
- [ ] 选 `getPresignedUrl` 本地实现可返回可访问的 `file://` 或 null 约定（需与架构师确认，见待确认）。

---

### 5.3 operatelog（操作日志）— 零依赖（可选复用 core Event）

**定位**：声明式操作日志的「逻辑内核」。本轮落地：日志模型、Handler SPI、默认日志 Handler、异步分发器、`@OperateLog` 注解定义。AOP 编织拦截器、真实持久化 Handler 不做。

**core 依赖事实**：`framework-core` **已存在** `core.event`（`EventBus` / `EventListener` / `FrameworkEvent`）。故 operatelog 可**可选**复用 core Event 做派发，但本轮主路径保持零依赖（用 `OperateLogDispatcher` + 虚拟线程/ExecutorService 直接聚合 Handler）。

#### 用户故事
```java
// 组装一条记录（真实注入由后续 AOP 完成，本轮仅提供模型/Handler）
OperateLogRecord record = OperateLogRecord.builder()
    .module("用户管理").action("CREATE").operator("admin")
    .status(OperateStatus.SUCCESS).costTime(12L).build();

// 异步分发：聚合多个 Handler，虚拟线程写入
dispatcher.dispatch(record);   // LoggingOperateLogHandler 打印日志
```

#### 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.operatelog` | 模型 | `OperateLogRecord` | traceId / module / action / description / content / operator / operatorId / status / errorMessage / costTime / operateTime / extra |
| `cn.jowen.framework.extras.operatelog` | 接口(SPI) | `OperateLogHandler` | `handle(record)` |
| `cn.jowen.framework.extras.operatelog.handler` | 实现 | `LoggingOperateLogHandler` | 默认实现，打印日志（可注入 `Logger`） |
| `cn.jowen.framework.extras.operatelog` | 类 | `OperateLogDispatcher` | 聚合 handlers + 异步写入（`Thread.ofVirtual()` 或 `ExecutorService`） |
| `cn.jowen.framework.extras.operatelog` | 注解 | `@OperateLog` | module / action / description / content / condition / async / extra（仅定义，零依赖） |
| `cn.jowen.framework.extras.operatelog` | 枚举 | `OperateStatus` | SUCCESS / FAIL |

> 可选（P2，待确认是否纳入本轮）：`OperateLogEvent extends FrameworkEvent` + 经 `core.event.EventBus` 派发。

#### 验收标准
- [ ] `LoggingOperateLogHandler.handle(record)` 不抛异常且日志可见（用 `ListAppender` 等断言）。
- [ ] `OperateLogDispatcher.dispatch(record)` 能并发调用所有已注册 Handler；`async=true` 时记录最终落盘。
- [ ] `OperateLogRecord` 必填字段 `module/action/operateTime` 非空（构建器/校验）。
- [ ] `@OperateLog` 注解可被成功保留（`@Retention(RUNTIME)`），后续 AOP 可读取。
- [ ] 单测可 `new OperateLogDispatcher(List.of(new LoggingOperateLogHandler()))` 直接跑。

---

### 5.4 datapermission（数据权限）— 零依赖（规则引擎，不绑 MyBatis）

**定位**：纯规则计算引擎——输入 `UserInfo` + `DataScope` + 表信息 → 输出 SQL WHERE 片段（如 `(dept_id = ? OR ...)`）。**不**做 MyBatis SQL 改写拦截器。依赖 `core.context.ContextCarrier`（已确认存在，`ContextKey.named(...)` + `get/set/runWith/snapshot/replay`）。

#### 用户故事
```java
// 规则计算（独立单测，无需 MyBatis）
DeptDataPermissionRule rule = new DeptDataPermissionRule();
UserInfo user = UserInfo.of(1L, 10L, List.of(10L, 11L, 12L), DataScope.DEPT_AND_CHILD);
String where = rule.getExpression(user, TableInfo.of("t_user", "dept_id"));
// -> "t_user.dept_id IN (?, ?, ?)" 或等价片段

// 上下文读写（基于 core ContextCarrier）
DataPermissionContext.setCurrentUser(user);
UserInfo current = DataPermissionContext.getCurrentUser();
```

#### 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.datapermission` | 接口 | `DataPermissionRule` | `getExpression(userInfo, tableInfo)` → SQL 片段 + 参数列表 |
| `cn.jowen.framework.extras.datapermission` | 枚举 | `DataScope` | ALL / DEPT_AND_CHILD / DEPT / SELF / CUSTOM |
| `cn.jowen.framework.extras.datapermission` | 模型 | `UserInfo` | userId / deptId / deptIds / dataScope（含 `of(...)` 工厂） |
| `cn.jowen.framework.extras.datapermission` | 模型 | `TableInfo`（建议新增） | tableName / deptColumn / userColumn（规则输入） |
| `cn.jowen.framework.extras.datapermission` | 类 | `DataPermissionContext` | 基于 `core.context.ContextCarrier` 的 `setCurrentUser/getCurrentUser/ignore/restore` |
| `cn.jowen.framework.extras.datapermission.rule` | 实现 | `DeptDataPermissionRule` | 本人/本部门/本部门及子部门/全部 → WHERE 片段 |
| `cn.jowen.framework.extras.datapermission.rule` | 实现 | `UserDataPermissionRule` | 仅本人数据（`create_by = ?`） |
| `cn.jowen.framework.extras.datapermission.rule` | 实现 | `CustomDataPermissionRule` | CUSTOM → 自定义表达式/参数 |

#### 验收标准
- [ ] `DEPT_AND_CHILD` 输出含 `deptId + deptIds` 所有值的 IN 条件；`DEPT` 仅本部门；`SELF` 仅 userId；`ALL` 返回空/恒真片段。
- [ ] 参数列表与 `?` 占位符数量一致，可被 `PreparedStatement` 绑定。
- [ ] 表别名/列名注入做白名单校验（防 SQL 注入，见待确认）。
- [ ] `DataPermissionContext.setCurrentUser(...)` 后可 `getCurrentUser()` 取回；`runWith` 作用域结束自动还原。
- [ ] 单测直接构造 `UserInfo` + `TableInfo` 调用规则，无需 Spring/MyBatis。

---

### 5.5 notification（消息通知）— 零依赖（本地 Handler）

**定位**：统一通知抽象 + Channel SPI；本轮仅 `ConsoleNotificationHandler` / `LogNotificationHandler` 两个本地调试实现。真实渠道（mail/SMS/钉钉/企微/Webhook）不做。

#### 用户故事
```java
NotificationRequest req = NotificationRequest.builder()
    .channel(NotificationChannel.EMAIL)   // 本地 Handler 仅打印，不真实发送
    .to("user@example.com").subject("Hi").content("Hello").build();

NotificationResult r = notificationService.send(req);  // Console/Log 输出，success=true
```

#### 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.notification` | 接口 | `NotificationService` | `send` / `sendAsync` / `sendBatch` |
| `cn.jowen.framework.extras.notification` | 模型 | `NotificationRequest` | channel / to / subject / content / templateCode / templateParams / attachments / extra |
| `cn.jowen.framework.extras.notification` | 模型 | `NotificationResult` | success / messageId / errorMessage / channel |
| `cn.jowen.framework.extras.notification` | 枚举 | `NotificationChannel` | EMAIL / SMS / DINGTALK / WECHAT_WORK / WEBHOOK / CUSTOM |
| `cn.jowen.framework.extras.notification.channel` | 接口(SPI) | `NotificationChannelHandler` | `send(request)` → `NotificationResult` |
| `cn.jowen.framework.extras.notification.channel` | 实现 | `ConsoleNotificationHandler` | 输出到 `System.out`（开发调试） |
| `cn.jowen.framework.extras.notification.channel` | 实现 | `LogNotificationHandler` | 写日志 |
| `cn.jowen.framework.extras.notification` | 实现(建议) | `LocalNotificationService` | 聚合 Handler（按 channel 路由） |

#### 验收标准
- [ ] `send` 经对应本地 Handler 返回 `success=true`，并在 stdout/日志可见内容。
- [ ] 未知/未注册 channel 时 `send` 返回 `success=false` + `errorMessage`（不抛 NPE）。
- [ ] `sendAsync` 返回 `CompletableFuture` 且最终完成。
- [ ] `sendBatch` 对每条请求分别返回结果（部分失败不影响其他）。
- [ ] 单测可直接 `new LocalNotificationService(List.of(new ConsoleNotificationHandler()))` 跑。

---

### 5.6 desensitize（数据脱敏 JSON 适配）— 依赖 core.desensitize + jackson(optional)

**定位**：**只做 Jackson 3 序列化链适配**，把脱敏挂到 JSON 输出。规则模型/执行器来自 `framework-core.desensitize`，本子包**不重新定义**脱敏规则。

**core 依赖事实（已核实）**：
- `core.desensitize.DesensitizeField`：`@DesensitizeField(strategy, startKeep, endKeep, replacement, skip)`，FIELD 目标，RUNTIME 保留
- `core.desensitize.DesensitizeStrategies`：PHONE / ID_CARD / BANK_CARD / EMAIL / NAME / ADDRESS / PASSWORD / FIXED_PHONE / LICENSE_PLATE / CUSTOM
- `Desensitizer.getInstance().maskObject(obj)`：反射处理 `@DesensitizeField`；`mask(text, strategy, ctx)`
- `core.desensitize.DesensitizeContext(startKeep, endKeep, replacement, skip)`：`skip()=true` 时原样返回（管理员免脱敏）

#### 用户故事
```java
public class UserVO {
    @PhoneDesensitize          // -> 解析为 @DesensitizeField(strategy="PHONE")
    private String phone;      // 序列化输出: 138****5678
    @IdCardDesensitize
    private String idCard;     // 输出: 110***********1234
}

// Jackson 3 注册 Module 后序列化
ObjectMapper mapper = JsonMapper.builder().addModule(new DesensitizeModule()).build();
String json = mapper.writeValueAsString(userVO);  // phone 字段已脱敏
```

#### 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.desensitize.annotation` | 注解 | `@PhoneDesensitize` | → `@DesensitizeField(strategy="PHONE")` |
| `cn.jowen.framework.extras.desensitize.annotation` | 注解 | `@IdCardDesensitize` | → `@DesensitizeField(strategy="ID_CARD")` |
| `cn.jowen.framework.extras.desensitize.annotation` | 注解 | `@BankCardDesensitize` | → strategy=BANK_CARD |
| `cn.jowen.framework.extras.desensitize.annotation` | 注解 | `@EmailDesensitize` | → strategy=EMAIL |
| `cn.jowen.framework.extras.desensitize.annotation` | 注解 | `@NameDesensitize` | → strategy=NAME |
| `cn.jowen.framework.extras.desensitize.annotation` | 注解 | `@AddressDesensitize` | → strategy=ADDRESS |
| `cn.jowen.framework.extras.desensitize.annotation` | 注解(建议) | `@CustomDesensitize` | → strategy=CUSTOM + startKeep/endKeep/replacement |
| `cn.jowen.framework.extras.desensitize.serializer` | 类(Jackson Module) | `DesensitizeModule` | `tools.jackson` Module，注册 `DesensitizeSerializerModifier` |
| `cn.jowen.framework.extras.desensitize.serializer` | 类 | `DesensitizeSerializerModifier` | `BeanSerializerModifier` 扫描字段注解替换序列化器 |
| `cn.jowen.framework.extras.desensitize.serializer` | 类 | `DesensitizeJsonSerializer` | 用 `Desensitizer` 脱敏后序列化输出 |

> pom 变更：`tools.jackson.core:jackson-databind`（optional，BOM 管理，无需 version）。缺失时 `DesensitizeModule` 类不编译/不装配。

#### 验收标准
- [ ] 标注 `@PhoneDesensitize` 的字段序列化 JSON 输出 `138****5678`（PHONE：前 3 后 4）。
- [ ] 标注 `@IdCardDesensitize` 输出前 3 后 4 掩码。
- [ ] 便捷注解等价解析为 `@DesensitizeField(strategy=...)`，最终走 `Desensitizer.maskObject/mask`。
- [ ] `@DesensitizeField(skip=true)`（或上下文 skip）字段原样输出（管理员免脱敏）。
- [ ] 非 String 字段 / 无注解字段原样序列化，不受影响。
- [ ] 单测：`new JsonMapper().addModule(new DesensitizeModule())` 序列化 VO 断言脱敏结果。

---

## 六、待确认问题（需架构师/工程师后续确认）

1. **operatelog 是否复用 core Event？**
   `framework-core` 已存在 `core.event`（`EventBus`/`EventListener`/`FrameworkEvent`）。本轮主路径建议保持零依赖（`OperateLogDispatcher` 直接聚合 Handler）；是否额外提供 `OperateLogEvent extends FrameworkEvent` + `EventBus` 派发作为可选路径？（影响 R-operatelog-4 是否纳入本轮）

2. **datapermission 列名/表名注入安全**
   规则引擎拼接 SQL 片段时，`TableInfo` 的 `tableName/deptColumn/userColumn` 来源需做**白名单/标识符校验**（防 SQL 注入）。白名单来源（注解常量 or 配置 or 调用方保证）需架构师定。

3. **storage `getPresignedUrl` 本地语义**
   本地磁盘无"预签名 URL"概念。本轮返回约定：`LocalFileStorage.getPresignedUrl` 返回 `file://` 绝对路径 or `null`？需与上层确认（建议返回 `file://` 路径并在文档标注限制）。

4. **desensitize 跳过上下文继承**
   管理员免脱敏基于 `core.desensitize.DesensitizeContext.skip()`，是单次 mask 传入，还是需经 `core.context.ContextCarrier` 共享？README 提"基于 core ContextCarrier 管理员跳过"，但 core `DesensitizeContext` 当前是方法参数式——跨切面（如 AOP 层）共享需确认载体。

5. **虚拟线程池边界（operatelog / 异步）**
   异步写入用 `Thread.ofVirtual().start(...)` 还是 `ExecutorService.newVirtualThreadPerTaskExecutor()`？是否需暴露线程池大小/拒绝策略配置（本轮轻量 Properties 是否包含）？

6. **各功能轻量 Properties POJO 范围（P1）**
   本轮仅在功能内放 Properties 还是统一到 `cn.jowen.framework.extras.config` 子包？自动装配上移决定 Properties 最终由 `framework-boot-autoconfigure` 绑定，本轮 POJO 仅作为数据载体，需确认是否前置创建。

7. **captcha `CaptchaResult.extra` 与 SLIDER/SMS 预留**
   本轮不做 SLIDER/SMS，但 `CaptchaType` 含枚举值、`CaptchaResult.extra` 预留。是否允许枚举先全量声明（仅 IMAGE/ARITHMETIC 有实现），还是收窄枚举？建议保留全量声明 + 未实现类型抛 `UnsupportedOperationException`。

---

## 七、交付约定总结

- **本轮产出**：6 子包的核心逻辑层源码（接口 + 本地/算法实现 + 模型/枚举/注解）+ 独立单测。
- **不产出**：AOP 拦截器、Redis 实现、自动装配层、云后端、真实渠道 Handler。
- **依赖**：仅 framework-core（compile）+ jackson-databind（optional，仅 desensitize）。
- **可验证性**：每个子包均可 `new` 核心类独立跑单测，零 Spring 容器依赖。
