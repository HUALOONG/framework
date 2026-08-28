# framework-demo

基于 `framework-boot-starter` 的示例应用：**零外部依赖、克隆即跑**。

演示框架的核心能力组合：数据访问（H2 内存库）、缓存（Caffeine）、国际化、
限流、日志脱敏、健康检查，并提供一个可视化示例页面。

> 环境要求：JDK 21+、Maven 3.9+。无需安装 MySQL / Redis / MinIO。

---

## 一、快速开始

```bash
# 1. 先安装框架到本地仓库（在框架根目录执行一次即可）
cd ..
mvn clean install -DskipTests

# 2. 启动本示例
cd framework-demo
mvn spring-boot:run
```

启动后访问：

| 入口 | 地址 |
|---|---|
| 示例页面 | http://localhost:8080/ |
| 健康检查 | http://localhost:8080/actuator/health |
| 用户列表 | http://localhost:8080/api/users |

**离线/无打包插件时的启动方式**（当 `spring-boot-maven-plugin` 不可用时）：

```bash
mvn -o clean compile
mvn -o dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/classes:$(cat target/cp.txt)" cn.jowen.framework.demo.DemoApplication
# Windows 用分号分隔：java -cp "target/classes;$(cat target/cp.txt)" ...
```

打包可执行 JAR：`mvn package`（输出 `target/framework-demo.jar`）。

---

## 二、项目结构

```text
framework-demo/
├── pom.xml                          # 依赖：framework-bom + Spring Boot BOM
└── src/main/
    ├── java/cn/jowen/framework/demo/
    │   ├── DemoApplication.java     # 启动入口
    │   ├── DataInitializer.java     # 建表 + 示例数据（ApplicationRunner）
    │   ├── config/
    │   │   ├── DemoCacheConfiguration.java  # 缓存参数定制（扩展点示例）
    │   │   ├── DesensitizeConfiguration.java # 脱敏规则注册（框架默认不内置）
    │   │   ├── GlobalExceptionHandler.java   # 框架异常 → 统一响应（应用层职责）
    │   │   └── MaskingPatternLayout.java     # Logback 脱敏接线
    │   ├── entity/User.java         # 实体（@Table / @Id / @Column / @GeneratedValue）
    │   ├── repository/UserRepository.java    # 数据访问（JdbcTemplate + RowMapper）
    │   ├── service/UserService.java          # 业务（缓存 + 数据访问）
    │   └── controller/
    │       ├── UserController.java  # 用户 CRUD + 限流
    │       └── DemoController.java  # i18n / 缓存统计 / 能力自检
    └── resources/
        ├── application.yml          # 全部配置集中在 framework.* 前缀下
        ├── logback-spring.xml       # 接入脱敏布局
        ├── messages.properties      # 默认语言资源
        ├── messages_zh_CN.properties
        ├── messages_en_US.properties
        └── static/index.html        # 示例页面
```

---

## 三、接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/users` | 用户列表 |
| GET | `/api/users/{id}` | 查询单个（命中缓存时不打印回源日志） |
| POST | `/api/users` | 新增（`@RateLimit` 每分钟 10 次） |
| PUT | `/api/users/{id}` | 更新并清除缓存 |
| DELETE | `/api/users/{id}` | 删除并清除缓存 |
| GET | `/api/demo/hello?name=Alice&locale=en_US` | 国际化 |
| GET | `/api/demo/cache-stats` | 缓存命中率 |
| GET | `/api/demo/ratelimit` | 限流演示（每分钟 5 次） |
| GET | `/api/demo/capabilities` | 装配自检 |

响应统一为 `{code, message, data, success}`，由框架 `Result` 提供。

---

## 四、关键配置

全部配置位于 `application.yml` 的 `framework.*` 前缀下：

```yaml
framework:
  data:
    type: jdbc                      # jdbc | mybatis
    datasource:
      url: jdbc:h2:mem:demodb;DB_CLOSE_DELAY=-1
      driver-class-name: org.h2.Driver
  cache:
    enabled: true
  i18n:
    basename: messages              # 对应 messages*.properties
    default-locale: zh_CN
  logger:
    desensitize-enabled: true
  extras:
    web.ratelimit.enabled: true
    storage:
      type: LOCAL                   # LOCAL | MINIO | OSS | S3
```

**切换数据库**（以 MySQL 为例）只需改配置并加驱动依赖：

```yaml
framework:
  data:
    datasource:
      url: jdbc:mysql://localhost:3306/demo
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver
```

---

## 五、扩展指引

框架装配均标注了 `@ConditionalOnMissingBean`，**业务 Bean 优先**：

| 想做什么 | 怎么做 |
|---|---|
| 覆盖缓存参数 | 在 `DemoCacheConfiguration` 中注册同名缓存（已示例） |
| 切 Redis 缓存 | 引入 redisson，`CaffeineCache` → `RedissonCache` |
| 换对象存储 | 改 `framework.extras.storage.type`，补 endpoint/密钥 |
| 加脱敏规则 | 在 `DesensitizeConfiguration` 中 `register()`（已示例） |
| 自定义限流响应 | 扩展 `GlobalExceptionHandler` 的 `@ExceptionHandler` |
| 开启数据权限 | `datapermission.enabled: true` + 实现 `DataPermissionRule` |
| 关闭某项能力 | 对应 `framework.xxx.enabled=false` |

---

## 六、使用框架时需注意的三点

这三条是实际接入时踩到的坑，框架侧已留出扩展点，但**需要应用显式处理**：

1. **日志脱敏需自行接线**
   框架提供 `LogMaskLayout` 装饰器，但不接管应用的 Appender。
   需像本示例那样配置 `logback-spring.xml` + 自定义 Layout，并**注册脱敏规则**
   （框架默认不内置任何 `DesensitizeRule`，不注册则 `mask()` 原样返回）。

2. **框架异常需应用层转换**
   限流/幂等/签名等切面校验失败时抛 `ExtrasException`，框架不预设
   HTTP 状态码映射。本示例的 `GlobalExceptionHandler` 给出一种推荐实现
   （`ExtrasException` → 400），可按业务调整。

3. **建表脚本不走 Spring 的 `spring.sql.init`**
   框架数据源由 `JdbcContext` 自管，与 Spring `DataSource` 无关，
   因此 `schema.sql` 不会自动执行。本示例在 `DataInitializer` 中用
   框架 `JdbcTemplate` 建表；生产环境建议改用 Flyway / Liquibase。

---

## 七、依赖说明

| 依赖 | 作用 |
|---|---|
| `framework-boot-starter` | 核心入口，含 core / logger / autoconfigure |
| `framework-data-jdbc` | 数据访问（JdbcTemplate、事务、方言） |
| `framework-cache` | 缓存（CacheManager、Caffeine、多级缓存） |
| `framework-i18n` | 国际化 |
| `framework-boot-web` | 限流 / 幂等 / 加解密 / 签名 / 验证码 / 操作日志 |
| `framework-extras-storage` | 文件存储 |
| `framework-extras-message` | 消息门面 |
| `caffeine` | **必须显式引入**：在 `framework-cache` 中是 optional |
| `h2` | 内存库，保证零外部依赖 |

> 按需裁剪：不需要某项能力时，直接移除对应依赖即可，装配层会按
> `@ConditionalOnClass` 自动跳过。
