# framework-extras 扩展能力 PRD（装配层 / 真实渠道 / 云后端 / AOP 拦截器）

> 文档元信息
> - **模块**：`framework-extras`（4 项新能力均落在此模块；Spring 装配上移至 `framework-boot-autoconfigure` 另行处理）
> - **本轮主题**：config 装配层轻量 POJO + notification 真实渠道 + storage 云后端 + @注解 AOP 拦截器
> - **基线**：Spring Boot 4.1.0 + Spring Cloud 2025.1.2 + Java 21 + Jackson 3（`tools.jackson.*`）
> - **前置依赖**：已交付的 6 项核心逻辑层（captcha / storage / operatelog / datapermission / notification / desensitize），本轮**不得修改/重命名/复写**这些类的任何代码
> - **包约定**：`cn.jowen.framework.extras.<feature>` 不变；新增的 channel/impl/aop 子包均以 `<feature>/channel/`、`<feature>/impl/`、`<feature>/aop/` 形式追加，避免与核心类碰撞

---

## 一、目标与范围

### 1.1 本轮定位

承接 `extras-core-logic-prd.md` / `extras-core-logic-design.md` 已交付的 6 项核心逻辑，本轮落地「增强层」——让每项能力的真实可用链路从「接口 + 本地实现」推进到「真实渠道/云后端 + 自动装配 + AOP 可切面」。不触碰已交付的接口、模型、枚举、注解、现有实现类。

### 1.2 本轮 4 项能力矩阵

| # | 能力 | 定位 | 关键约束 |
|:-:|:-----|:-----|:---------|
| C1 | config 装配层（轻量 Properties POJO + `@EnableExtrasXxx`） | 为每项已有能力补一个**标记性注解**（`@EnableCaptcha` / `@EnableStorage` / …），把已有的 `XxxProperties` POJO 暴露为容器 Bean，并注册本地 Handler/存储实现 | 不写 `@ConfigurationProperties`；注解本身零依赖（仅 `org.springframework`） |
| C2 | notification 真实渠道 | 每个真实渠道独立 optional 依赖，按 channel 路由到对应 `NotificationChannelHandler` 实现 | `jakarta.mail` / `alibabacloud-dysmsapi` / `dingtalk` SDK / `wechat-work-sdk` / `okhttp` Webhook 均为 `<optional>true</optional>` |
| C3 | storage 云后端 | 每个云存储独立 optional 依赖，实现 `FileStorage` 接口 | `minio` / `aliyun-oss-sdk` / `aws-sdk` 均为 `<optional>true</optional>` |
| C4 | @注解 AOP 拦截器 | 依赖 `spring-boot-starter-aop`（optional），为 `@OperateLog` / `@DataPermission`（新增）编织切面 | 对 `@OperateLog` 只做**异步聚合 dispatch**；对 `@DataPermission` 只做**上下文设置**；**不做** MyBatis SQL 改写 |

### 1.3 与已交付 6 项的关系

```
已交付（不动）                本轮新增（增强）
─────────────────────          ─────────────────────────────
captcha: CaptchaService        → @EnableCaptcha 注册 Bean
        LocalCaptchaService
        CaptchaProperties

storage: FileStorage           → @EnableStorage 注册 Bean
        LocalFileStorage       → MinIO/OSS/S3 实现（optional）
        StorageProperties

notification: NotificationService   → @EnableNotification 注册 Bean
        LocalNotificationService     → Email/SMS/钉钉/企微/Webhook Handler（optional）
        NotificationProperties

operatelog: OperateLogDispatcher     → @EnableOperateLog 注册 Dispatcher
        @OperateLog（不动）            → AOP 切面 (optional)
        OperateLogProperties

datapermission: DataPermissionRule   → @EnableDataPermission 注册 Bean
        DataPermissionContext                → AOP 切面 (optional)
        DataPermissionProperties
        (新增 @DataPermission 注解)

desensitize: DesensitizeModule          → 本轮不触碰
        便捷注解
```

---

## 二、依赖策略（关键决策）

### 2.1 config 装配层 — 依赖边界

```
framework-extras (compile)
├── framework-core (compile，已有)
└── spring-boot-autoconfigure (optional，本轮新增)
    用途：仅在 ExtrasConfigEnabler.java 中标注 @AutoConfiguration、
          @ConditionalOnClass、@Configuration 等；不引入 @ConfigurationProperties 绑定逻辑
```

**为什么 `spring-boot-autoconfigure` 为 optional**：保持「extras 核心层不强制依赖 Boot」，仅在用户想使用 `@EnableXxx` 时引入；不引入的用户可零接触该依赖。

### 2.2 notification 真实渠道 — 每个渠道独立 optional

| 渠道 | 依赖坐标 | scope | 备注 |
|:-----|:---------|:------|:-----|
| EMAIL | `org.eclipse.angus:angus-mail`（Jakarta Mail 2.x） | optional | Spring Boot 4 用 jakarta 命名空间 |
| SMS（阿里云） | `com.aliyun:dysmsapi20170525` | optional | 版本由 BOM 管理，不写 version |
| 钉钉 | `com.dingtalk.open:dingtalk-stream` 或 `dingtalk-sdk` | optional | 任选一种，推荐 `dingtalk-sdk` |
| 企业微信 | `com.github.wechatpay-apiv3:wechatpay-java` 以外的专用 SDK | optional | 使用企业微信官方 REST SDK |
| WEBHOOK | `com.squareup.okhttp3:okhttp` | optional | 通用 HTTP |

> **原则**：用户未加对应依赖时，`ChannelRegistrar` 自动跳过该渠道注册，启动不报错（`@ConditionalOnClass`）。

### 2.3 storage 云后端 — 每云独立 optional

| 后端 | 依赖坐标 | scope | 备注 |
|:-----|:---------|:------|:-----|
| MinIO | `io.minio:minio` | optional | 推荐 8.x |
| 阿里云 OSS | `com.aliyun.oss:aliyun-sdk-oss` | optional | 版本由 BOM 管理 |
| AWS S3 | `software.amazon.awssdk:s3` | optional | BOM 管理 |

### 2.4 @注解 AOP 拦截器 — 依赖 spring-boot-starter-aop（optional）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
    <optional>true</optional>
</dependency>
```

> **说明**：AOP 切面是纯框架内部能力，不对外部业务代码产生传导依赖——只要 `framework-extras` 是 optional 给下游，`spring-boot-starter-aop` 也是 optional，不会强拉。

### 2.5 本轮新增 pom 依赖汇总

| 依赖 | scope | 说明 |
|:-----|:------|:-----|
| `org.springframework.boot:spring-boot-autoconfigure` | optional | config 装配层标记注解使用 |
| `org.springframework.boot:spring-boot-starter-aop` | optional | AOP 拦截器使用 |
| `org.eclipse.angus:angus-mail` | optional | Email 渠道 |
| `com.aliyun:dysmsapi20170525` | optional | 阿里云 SMS |
| `com.dingtalk.open:dingtalk-sdk` | optional | 钉钉 |
| `com.tencent.workchat:workchat-sdk` | optional | 企微（具体坐标以 BOM 确认为准） |
| `com.squareup.okhttp3:okhttp` | optional | Webhook HTTP |
| `io.minio:minio` | optional | MinIO |
| `com.aliyun.oss:aliyun-sdk-oss` | optional | 阿里云 OSS |
| `software.amazon.awssdk:s3` | optional | AWS S3 |

> **所有新依赖均 `<optional>true</optional>`，不写 `<version>`，版本由 `framework-bom` 统一管理**。

---

## 三、各能力详细 PRD

### 3.1 Config 装配层（`@EnableXxx`）

#### 3.1.1 产品目标

让每项能力在 Spring 容器内**一键可用**：引入 `framework-extras` 后，只需在启动类上加 `@EnableXxx`，即可把现有的 `XxxProperties` + 本地 Handler/存储注册为 Bean。

#### 3.1.2 用户故事

```java
// 用户视角
@SpringBootApplication
@EnableExtras  // 一次性开启所有 extras 能力
public class MyApp { ... }

// 或按需开启
@EnableCaptcha      // 只开启验证码
@EnableStorage      // 只开启文件存储
@EnableNotification // 只开启通知
```

#### 3.1.3 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.config` | 注解 | `@EnableExtras` | 总开关：组合所有 `@Import` |
| `cn.jowen.framework.extras.config` | 配置类 | `ExtrasBootstrapConfiguration` | 被 `@EnableExtras` 引入；条件化注册各组件 |
| `cn.jowen.framework.extras.captcha.config` | 注解 | `@EnableCaptcha` | 开启验证码（组合式：`@ConditionalOnClass(LocalCaptchaService.class)`） |
| `cn.jowen.framework.extras.storage.config` | 注解 | `@EnableStorage` | 开启文件存储 |
| `cn.jowen.framework.extras.notification.config` | 注解 | `@EnableNotification` | 开启通知 |
| `cn.jowen.framework.extras.operatelog.config` | 注解 | `@EnableOperateLog` | 开启操作日志 dispatcher |
| `cn.jowen.framework.extras.datapermission.config` | 注解 | `@EnableDataPermission` | 开启数据权限 |
| `cn.jowen.framework.extras.desensitize.config` | 注解 | `@EnableDesensitize` | 开启脱敏（仅注册 Module，不动现有 desensitize 实现） |

> **规则**：所有 `@EnableXxx` 注解均为**组合注解**，内部含 `@Import(XxxBootstrapConfiguration.class)` + `@ConditionalOnClass`，**不直接写 `@Bean` 方法**（Bean 定义留在对应的 `XxxBootstrapConfiguration` 中，方便测试覆盖）。

#### 3.1.4 `ExtrasBootstrapConfiguration` 伪代码契约

```java
@NullMarked
@Configuration
@ConditionalOnClass(name = {
    "cn.jowen.framework.extras.captcha.CaptchaService",
    "cn.jowen.framework.extras.storage.FileStorage",
    "cn.jowen.framework.extras.notification.NotificationService",
    "cn.jowen.framework.extras.operatelog.OperateLogHandler",
    "cn.jowen.framework.extras.datapermission.DataPermissionRule",
    "cn.jowen.framework.extras.desensitize.serializer.DesensitizeModule"
})
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", matchIfMissing = true)
public class ExtrasBootstrapConfiguration {

    // 各子能力独立 ConditionalOnClass，互不影响
    @Bean @ConditionalOnClass(LocalCaptchaService.class)
    public CaptchaService jowenCaptchaService(CaptchaProperties props) { ... }

    @Bean @ConditionalOnClass(LocalFileStorage.class)
    public FileStorage jowenFileStorage(StorageProperties props) { ... }

    @Bean @ConditionalOnClass(LocalNotificationService.class)
    public NotificationService jowenNotificationService(List<NotificationChannelHandler> handlers) { ... }

    @Bean @ConditionalOnClass(OperateLogDispatcher.class)
    public OperateLogDispatcher jowenOperateLogDispatcher(List<OperateLogHandler> handlers) { ... }

    // ... 其余类似
}
```

#### 3.1.5 验收标准

- [ ] 引入 `framework-extras` 且 classpath 有 Spring Boot，加 `@EnableExtras` 后能注册全部本地 Bean。
- [ ] `framework.extras.enabled=false` 时，Bean 均不注册（`@ConditionalOnProperty`）。
- [ ] 未引入某 optional 依赖（如未加 `minio`）时，对应 `@EnableXxx` 不抛异常；仅注册本地实现。
- [ ] 各 `@EnableXxx` 注解 `@Retention(RUNTIME)` 可被反射识别。
- [ ] `CaptchaProperties` / `StorageProperties` / … 作为 POJO 注册到容器，可供 `application.yml` 配置。

#### 3.1.6 Out of Scope

- `@ConfigurationProperties` 绑定语义**本轮不在 extras 内**实现（由框架架构师在 `framework-boot-autoconfigure` 层统一处理，本轮仅提供 POJO 载体）。
- 不定义 `@Bean` 级别的校验逻辑（JSR-380）。
- 不编写任何 Spring Test 集成测试（留给 boot-autoconfigure 子模块）。

---

### 3.2 notification 真实渠道

#### 3.2.1 产品目标

在保留 `ConsoleNotificationHandler` / `LogNotificationHandler` 的基础上，**按需**新增 Email / SMS / 钉钉 / 企微 / Webhook 五种真实渠道，每种渠道独立 optional 依赖、互不耦合。

#### 3.2.2 用户故事

```java
// 场景：仅引入 email 依赖
NotificationRequest req = NotificationRequest.builder()
    .channel(NotificationChannel.EMAIL)
    .to("user@example.com")
    .subject("欢迎").content("Hello").build();
NotificationResult r = notificationService.send(req); // success=true
```

```java
// 场景：webhook 通用回调
NotificationRequest req = NotificationRequest.builder()
    .channel(NotificationChannel.WEBHOOK)
    .to("https://hooks.slack.com/...")
    .content("{\"text\":\"alert\"}")
    .build();
```

#### 3.2.3 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.notification.channel` | 实现 | `EmailNotificationHandler` | 调用 Jakarta Mail 发送；依赖 `angus-mail`（optional） |
| `cn.jowen.framework.extras.notification.channel` | 实现 | `SmsNotificationHandler` | 调用阿里云 SMS SDK；依赖 `dysmsapi20170525`（optional） |
| `cn.jowen.framework.extras.notification.channel` | 实现 | `DingTalkNotificationHandler` | 调用钉钉机器人/应用消息；依赖 `dingtalk-sdk`（optional） |
| `cn.jowen.framework.extras.notification.channel` | 实现 | `WeChatWorkNotificationHandler` | 调用企微应用消息；依赖 `workchat-sdk`（optional） |
| `cn.jowen.framework.extras.notification.channel` | 实现 | `WebhookNotificationHandler` | HTTP POST 到任意 URL；依赖 `okhttp`（optional） |
| `cn.jowen.framework.extras.notification.config` | 配置类 | `NotificationChannelAutoConfiguration` | `@ConditionalOnClass` 注册上述 Handler Bean |

> **每个 Handler 的构造函数接收渠道专属的配置 POJO**（见 `EmailProperties` / `SmsProperties` / …），这些 POJO 放在同包内，作为普通 POJO 不标 `@ConfigurationProperties`。

#### 3.2.4 渠道专属 Properties POJO 列表

| POJO 类 | 所在包 | 用途 |
|:---------|:-------|:-----|
| `EmailProperties` | `extras.notification.config` | host / port / from / username / password / ssl / properties 映射 |
| `SmsProperties` | `extras.notification.config` | accessKeyId / accessKeySecret / signName / templateCode |
| `DingTalkProperties` | `extras.notification.config` | appKey / appSecret / webhookUrl / msgType |
| `WeChatWorkProperties` | `extras.notification.config` | corpId / agentId / secret / webhookUrl |
| `WebhookProperties` | `extras.notification.config` | baseUrl / timeout / headers 映射 |

#### 3.2.5 验收标准

- [ ] 引入 `angus-mail` 后，`EmailNotificationHandler` 能被注册（`@ConditionalOnClass(MimeMessage.class)`）。
- [ ] 未引入 `dysmsapi` 时，`SmsNotificationHandler` 不注册，启动无报错。
- [ ] 每个 Handler 的 `send()` 在缺少必要配置字段（如 `email.from` 未填）时，返回 `NotificationResult(success=false, errorMessage="...")`，**不抛异常**。
- [ ] `WebhookNotificationHandler` 能发起 HTTP POST 到任意 URL，并返回响应状态码。
- [ ] `LocalNotificationService` 在未知 channel（无 Handler 注册）时仍返回 `success=false`，不崩溃。
- [ ] 每个 Handler 至少有一个**不依赖 Spring 容器**的 JUnit5 + AssertJ 单测（直接 new Handler + mock 请求）。

#### 3.2.6 Out of Scope

- 模板引擎（FreeMarker / Thymeleaf）集成。
- 消息撤回 / 发送状态回调查询。
- 多渠道合并为一个"统一消息"抽象（本轮按 channel 各自独立）。
- 发送配额 / 限流控制。

---

### 3.3 storage 云后端

#### 3.3.1 产品目标

在保留 `LocalFileStorage` 的基础上，**按需**新增 MinIO / 阿里云 OSS / AWS S3 三种云存储实现，各实现独立 optional 依赖，实现同一 `FileStorage` 接口，可被 `FileStorageManager` 按名称注册并切换。

#### 3.3.2 用户故事

```java
// 用户按需引入 minio 依赖后，通过 application.yml 配置
FileStorage storage = storageManager.getStorage("minio");
FileInfo info = storage.upload(inputStream, "report.pdf");
String url = storage.getPresignedUrl(info.getObjectName()); // 返回真实预签名 URL
```

#### 3.3.3 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.storage.impl` | 实现 | `MinioFileStorage` | MinIO Java SDK 适配；依赖 `minio`（optional） |
| `cn.jowen.framework.extras.storage.impl` | 实现 | `AliyunOssFileStorage` | 阿里云 OSS SDK 适配；依赖 `aliyun-sdk-oss`（optional） |
| `cn.jowen.framework.extras.storage.impl` | 实现 | `AwsS3FileStorage` | AWS S3 SDK v2 适配；依赖 `aws-sdk`（optional） |
| `cn.jowen.framework.extras.storage.config` | 配置类 | `StorageCloudAutoConfiguration` | `@ConditionalOnClass` 注册上述 Bean |
| `cn.jowen.framework.extras.storage.config` | POJO | `MinioProperties` | endpoint / bucket / accessKey / secretKey / region |
| `cn.jowen.framework.extras.storage.config` | POJO | `AliyunOssProperties` | endpoint / bucket / accessKeyId / accessKeySecret |
| `cn.jowen.framework.extras.storage.config` | POJO | `AwsS3Properties` | region / bucket / accessKey / secretKey / endpoint (可选) |

#### 3.3.4 `FileStorageManager` 扩展契约

```java
// 现有接口不动，新增：
public class FileStorageManager implements InitializingBean {
    // 现有方法保持不变
    public FileStorage getStorage() { ... }
    public FileStorage getStorage(String name) { ... }
    public void registerStorage(String name, FileStorage storage) { ... }

    // 本轮新增：批量注册云后端
    public void registerAll(CloudStorageRegistry registry) { ... }
}
```

> **注意**：`FileStorageManager` 现有方法签名**不可修改**；新增方法可重载或新增，但**不破坏**已有调用方。若现有实现为静态工具类，本轮改为支持多注册名的单例管理器。

#### 3.3.5 验收标准

- [ ] `MinioFileStorage` 在 `minio` 依赖存在时，可通过 `registerStorage("minio", storage)` 注册。
- [ ] 未引入 `minio` 依赖时，类不存在，`@ConditionalOnClass` 保证不注册，启动无错。
- [ ] 每个云后端实现：`upload` → `download` → 字节比对一致；`delete` 后 `exists` 为 false。
- [ ] `getPresignedUrl` 在云后端返回真实可访问 URL（非 null）。
- [ ] 每个云后端实现至少一个**不依赖 Spring 容器**的 JUnit5 + AssertJ 单测（可 mock 网络或用 Testcontainers / WireMock，见待确认）。
- [ ] `StorageProperties` 作为 POJO 可被 Spring 绑定（由 config 装配层完成）。

#### 3.3.6 Out of Scope

- 断点续传 / 分片上传。
- CDN 回源 / 缓存预热。
- 图片处理（水印、缩略图）。
- 多 Bucket / 多 Region 路由策略。

---

### 3.4 @注解 AOP 拦截器

#### 3.4.1 产品目标

让 `@OperateLog` / 新 `@DataPermission` 注解**真正生效**：在方法执行前后自动组装 `OperateLogRecord` 并 dispatch；在权限查询方法执行前自动把 `UserInfo` 注入 `DataPermissionContext`。

#### 3.4.2 用户故事

```java
// 操作日志
@OperateLog(module = "用户管理", action = "CREATE", description = "创建用户")
public void createUser(UserDTO dto) { ... }

// 数据权限
@DataPermission(tableName = "t_user", deptColumn = DataPermissionColumns.DEPT_COLUMN)
public List<UserVO> listUsers() { ... }
// 方法执行前自动把当前用户写入 DataPermissionContext
```

#### 3.4.3 类清单

| 包路径 | 类型 | 类名 | 职责 |
|:-------|:-----|:-----|:-----|
| `cn.jowen.framework.extras.operatelog.aop` | 切面 | `OperateLogAspect` | 环绕 `@OperateLog` 方法：收集参数/返回值/耗时/异常 → `OperateLogRecord` → `dispatcher.dispatch()` |
| `cn.jowen.framework.extras.datapermission.aop` | 切面 | `DataPermissionAspect` | 拦截 `@DataPermission` 方法，将 `DataPermissionContext.setCurrentUser(ctx.getCurrentUser())` |
| `cn.jowen.framework.extras.datapermission` | 注解 | `@DataPermission` | 新增注解；`tableName` / `deptColumn` / `userColumn`（默认从 `DataPermissionColumns` 常量取） |

#### 3.4.4 `@OperateLogAspect` 伪代码契约

```java
@NullMarked
@Aspect
@Component
@ConditionalOnClass(OperateLog.class)
@ConditionalOnBean(OperateLogDispatcher.class)
public class OperateLogAspect {

    private final OperateLogDispatcher dispatcher;

    @Around("@annotation(oplog)")
    public Object around(ProceedingJoinPoint pjp, OperateLog oplog) throws Throwable {
        long start = System.currentTimeMillis();
        OperateStatus status;
        String errorMsg = null;
        Object result;
        try {
            result = pjp.proceed();
            status = OperateStatus.SUCCESS;
        } catch (Throwable t) {
            status = OperateStatus.FAIL;
            errorMsg = t.getMessage();
            throw t;
        } finally {
            OperateLogRecord record = buildRecord(pjp, oplog, result, status, errorMsg, System.currentTimeMillis() - start);
            if (oplog.async()) {
                dispatcher.dispatch(record);
            } else {
                // 同步：直接调用各 handler
                dispatcher.dispatchSync(record); // 需新增此方法（见下文）
            }
        }
        return result;
    }
}
```

#### 3.4.5 `OperateLogDispatcher` 新增方法

```java
public class OperateLogDispatcher {
    // 现有方法：dispatch(record) 异步
    // 本轮新增：
    public void dispatchSync(OperateLogRecord record) {
        // 在当前线程同步调用所有 Handler，用于 AOP 非 async 场景
        for (OperateLogHandler h : handlers) {
            h.handle(record);
        }
    }
}
```

> **注意**：`OperateLogDispatcher` 是已交付类，新增方法**不影响**已有 API；保持向后兼容。

#### 3.4.6 `@DataPermission` 注解定义

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {
    /** 表名（全限定或短名，由 TableInfo 白名单校验）。 */
    String tableName();
    /** 部门列名，缺省 DataPermissionColumns.DEPT_COLUMN。 */
    String deptColumn() default DataPermissionColumns.DEPT_COLUMN;
    /** 用户列名，缺省 DataPermissionColumns.USER_COLUMN。 */
    String userColumn() default DataPermissionColumns.USER_COLUMN;
}
```

#### 3.4.7 验收标准

- [ ] 标注 `@OperateLog(async=false)` 的方法执行后，`LoggingOperateLogHandler` 同步记录日志。
- [ ] 标注 `@OperateLog(async=true)` 的方法执行后，日志异步落盘（单测中用 `CountDownLatch` 等待）。
- [ ] 方法抛异常时，`status=FAIL`、`errorMessage` 非空，日志仍被记录。
- [ ] 标注 `@DataPermission` 的方法执行前，`DataPermissionContext.getCurrentUser()` 非空。
- [ ] 切面不拦截非标记方法。
- [ ] 单测：`new OperateLogAspect(dispatcher)` 直接实例化后通过 `AopTestUtils.getAdvice` 或 Spring AOT 模式跑，**不要求完整 Spring 容器**（可只用 `AnnotationConfigApplicationContext`）。

#### 3.4.8 Out of Scope

- MyBatis SQL 改写（`DataPermissionInterceptor` 拦截 `PreparedStatement`）**本轮不做**（见 README 明确列为后续）。
- `Lock` / `RateLimit` / `Idempotent` 的 AOP 拦截器（本轮只处理 `@OperateLog` 和 `@DataPermission`）。
- 切面顺序控制（`@Order`），默认最后执行。
- 多 `@OperateLog` 叠加场景。

---

## 四、接口契约与向后兼容

### 4.1 零破坏原则（硬性约束）

- **已交付类**（`CaptchaService` / `LocalCaptchaService` / `FileStorage` / `LocalFileStorage` / `OperateLogDispatcher` / `@OperateLog` / `DataPermissionRule` / `LocalNotificationService` / `DesensitizeModule` 等）**不修改任何现有类名、包名、方法签名、字段**。
- 新增类一律放在 `channel/`、`impl/`、`aop/`、`config/` 子包下。
- `FileStorageManager` 若现有实现已是接口 + 实现，本轮不重写；仅在实现类中**新增方法**（或新增 `CloudStorageRegistry` SPI 接口）并向下兼容。

### 4.2 接口扩展原则

| 已有接口 | 本轮扩展方式 |
|:---------|:------------|
| `NotificationChannelHandler` | 新增 `default boolean supports(NotificationChannel channel)` 默认返回 false，各真实渠道 Handler 覆盖返回 true |
| `FileStorage` | 不新增方法（现有 `upload/download/delete/exists/getFileInfo/listObjects/getPresignedUrl` 足够） |
| `OperateLogDispatcher` | 新增 `dispatchSync(OperateLogRecord)` 方法，默认实现为同步循环调用 handlers |
| `DataPermissionRule` | 不修改；新增 `@DataPermission` 注解，由 AOP 切面负责上下文注入 |

### 4.3 optional 依赖的运行时行为

- 未引入某 optional SDK 时，对应的 `@ConditionalOnClass` 为 false，Handler/Storage 实现**不注册**，不占容器资源。
- `LocalNotificationService` / `FileStorageManager` 在运行时检查到渠道/后端未注册时，返回 `success=false` 而非抛异常。

---

## 五、待确认问题（供主理人拍板）

### Q1. `FileStorageManager` 现有实现形态确认
- 需工程师确认 `FileStorageManager` 目前是否为接口 + 单例实现。**若已是 `@Component`**，本轮直接在 `ExtrasBootstrapConfiguration` 中 `@Autowired` 使用；**若是工具类/静态工厂**，本轮需将其改为支持多注册名的 Bean（**注意改动范围：仅 FileStorageManager，不动 FileStorage 接口**）。
- **建议方案**：保留现有实现；新增 `CloudStorageRegistry` 接口 + `DefaultCloudStorageRegistry` 实现，由 `ExtrasBootstrapConfiguration` 注册并注入 `FileStorageManager`，通过 `manager.registerStorage(name, cloudStorage)` 接入云后端。

### Q2. `OperateLogDispatcher.dispatchSync()` 是否需要单独暴露？
- 异步场景用现有 `dispatch()`；同步场景（`@OperateLog(async=false)`）需要同步调用。
- **建议**：新增 `dispatchSync()` 方法（见 3.4.5），**不修改**现有 `dispatch()` 行为，向后兼容。

### Q3. AOP 切面是否应独立放在 `framework-extras` 子包 `aop/`？
- 方案 A（推荐）：AOP 切面放在 `framework-extras/src/main/java/cn/jowen/framework/extras/*/aop/` 下，由 `ExtrasBootstrapConfiguration` 统一 `@Import`。
- 方案 B：拆出 `framework-extras-aop` 独立子模块（后续考虑，本轮不做）。
- **建议选 A**，本轮保持模块精简。

### Q4. 云存储单测的「网络 Mock」策略
- 直接连真实 MinIO/OSS/S3 不适合 CI。
- **建议**：单测用 `WireMock`（HTTP 协议）mock 阿里云 OSS / AWS S3 / MinIO 的 REST API；`minio` SDK 本身基于 HTTP，可统一 mock。`framework-extras/pom.xml` 新增 `wiremock` 依赖（`test` scope）。
- **备选**：使用 `Testcontainers` 起一个 MinIO 容器（更贴近真实，但启动慢）。
- **建议选 WireMock**（轻量、快），由主理人确认。

### Q5. `@EnableXxx` 注解的粒度：总开关还是分项？
- 方案 A（推荐）：一个总注解 `@EnableExtras`，内部 `@Import(ExtrasBootstrapConfiguration.class)`；不再提供分项注解（降低复杂度）。
- 方案 B：同时提供 `@EnableCaptcha` 等分项注解，供需要精细控制的场景使用。
- **建议选 A**，本轮先做总开关；后续按需再加分项。
- 若选 B，需定义 6 个 `@EnableXxx` + 6 个 `XxxBootstrapConfiguration`。

---

## 六、交付清单

| 编号 | 交付物 | 路径（相对 `framework-extras/src/main/java`） |
|:----|:-------|:---------------------------------------------|
| D1 | `config/ExtrasBootstrapConfiguration.java` + `@EnableExtras.java` | `cn/jowen/framework/extras/config/` |
| D2 | 5 种 notification 真实渠道 Handler + 5 个 Properties POJO + `NotificationChannelAutoConfiguration` | `cn/jowen/framework/extras/notification/channel/`、`config/` |
| D3 | 3 种 storage 云后端实现 + 3 个 Properties POJO + `StorageCloudAutoConfiguration` + `CloudStorageRegistry` 接口 | `cn/jowen/framework/extras/storage/impl/`、`config/` |
| D4 | `OperateLogAspect.java` + `DataPermissionAspect.java` + `@DataPermission.java` + `OperateLogDispatcher.dispatchSync()` 新增 | `cn/jowen/framework/extras/operatelog/aop/`、`datapermission/aop/` |
| D5 | `framework-extras/pom.xml` 新增 optional 依赖列表 | `pom.xml` |
| D6 | 每项能力的 JUnit5 + AssertJ 单测（不依赖 Spring 容器） | `src/test/java/cn/jowen/framework/extras/` 对应子包 |

---

## 七、优先级排序（P0 / P1 / P2）

| 编号 | 需求 | 优先级 |
|:----|:-----|:-------|
| R-config-1 | `@EnableExtras` + `ExtrasBootstrapConfiguration` 注册全部本地 Bean | **P0** |
| R-config-2 | `XxxProperties` 暴露为容器 Bean（5 个） | **P0** |
| R-notif-1 | `EmailNotificationHandler` + `EmailProperties` | **P0** |
| R-notif-2 | `WebhookNotificationHandler` + `WebhookProperties` | **P0** |
| R-notif-3 | `SmsNotificationHandler` / `DingTalkNotificationHandler` / `WeChatWorkNotificationHandler` | **P1** |
| R-storage-1 | `MinioFileStorage` + `MinioProperties` | **P0** |
| R-storage-2 | `AliyunOssFileStorage` + `AliyunOssProperties` | **P1** |
| R-storage-3 | `AwsS3FileStorage` + `AwsS3Properties` | **P2** |
| R-aop-1 | `OperateLogAspect`（环绕 `@OperateLog`，同步 + 异步） | **P0** |
| R-aop-2 | `OperateLogDispatcher.dispatchSync()` 新增方法 | **P0** |
| R-aop-3 | `@DataPermission` 注解 + `DataPermissionAspect` | **P1** |

---

## 八、风险与约束

1. **Jackson 3 迁移**：desensitize 模块已依赖 Jackson 3（`tools.jackson.*`），本轮 config 装配层**不新增 Jackson 依赖**（保持现状）。
2. **虚拟线程上下文传播**：`OperateLogDispatcher` 用虚拟线程池，若未来要透传 `ContextCarrier` 上下文，需显式 `ContextSnapshot.capture()/replay()`；本轮 AOP 切面在**主线程**同步场景下无此问题。
3. **AOP 与 `@ConditionalOnBean`**：`OperateLogAspect` 依赖 `OperateLogDispatcher` Bean，若 dispatcher 未注册（`framework.extras.enabled=false`），切面不激活，不报错。
4. **optional 依赖版本冲突**：`minio`、`aliyun-sdk-oss`、`aws-sdk` 各自带不同 HTTP 客户端，需注意传递依赖冲突；建议在 `framework-bom` 中统一管理版本。
