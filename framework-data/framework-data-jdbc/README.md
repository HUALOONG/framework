# framework-data-jdbc 模块架构设计

> 文档元信息
> - **模块**：framework-data-jdbc
> - **关键词**：JDBC 模板、连接池、事务、方言、结果集映射、SQL 拦截器
> - **描述**：framework-data-core 的轻量 JDBC 实现，封装 SQL 构建执行、结果集映射、连接池、事务、方言
> - **基线**：Spring Boot 4.x + Java 21（虚拟线程连接池调优、Jackson 3、Micrometer 2.0）

---

## 一、模块定位

`framework-data-jdbc` 是 framework-data-core 的 **JDBC 轻量实现（L2）**，落地仓储、SQL 构建执行、结果集映射、连接池管理、事务管理、方言适配，同时提供
SQL 拦截器链（日志/性能/多租户）与异常翻译。

**核心价值**：

| 场景     | 没有本模块                        | 有本模块                                |
|:---------|:----------------------------------|:----------------------------------------|
| SQL 执行 | 手写 Connection/PreparedStatement | JdbcTemplate 一行执行                   |
| 结果映射 | 手写 ResultSet 遍历               | BeanPropertyRowMapper 自动映射          |
| 连接管理 | 手动获取/释放连接                 | 连接池 + 代理 + 泄漏检测                |
| 多数据库 | SQL 方言硬编码                    | DialectRegistry 自动切换                |
| 监控     | 无 SQL 耗时统计                   | PerformanceInterceptor + Micrometer 2.0 |

**与核心模块的边界**：

| 模块                    | 定位          | 特点                       |
|:------------------------|:--------------|:---------------------------|
| **framework-data-jdbc** | **JDBC 实现** | **轻量、无 ORM、SQL 可控** |
| framework-data-core     | 抽象契约      | 本模块实现其全部接口       |
| framework-data-mybatis  | MyBatis 实现  | 二选一（或并存）           |

---

## 二、功能清单与依赖矩阵

| 功能           | 子包        | 核心依赖  | 可选依赖                                  |
|:---------------|:------------|:----------|:------------------------------------------|
| JDBC 核心模板  | core        | data-core | —                                         |
| 连接池管理     | connection  | —         | HikariCP / Druid                          |
| Statement 构建 | statement   | —         | —                                         |
| 结果集映射     | mapping     | —         | Jackson 3（JSON 字段）                    |
| 方言实现       | dialect     | data-core | MySQL/PostgreSQL/Oracle/SQLServer/H2 驱动 |
| 事务实现       | transaction | —         | —                                         |
| 仓储实现       | repository  | —         | —                                         |
| SQL 拦截器     | interceptor | —         | —                                         |
| 异常转换       | exception   | data-core | —                                         |
| 配置           | config      | —         | —                                         |
| 工具           | util        | —         | —                                         |

---

## 三、整体包结构

```text
framework-data-jdbc
└─ src/main/java/com/framework/data/jdbc/
   ├─ core/          # JdbcTemplate / NamedParameterTemplate / BatchTemplate / SqlRunner
   ├─ connection/    # ConnectionProvider / HikariConnectionProvider / DruidConnectionProvider
   ├─ statement/     # PreparedStatementBuilder / SqlBuilder / ParameterBinder
   ├─ mapping/       # BeanPropertyRowMapper / DefaultTypeHandlers / CamelCaseNamingStrategy
   ├─ dialect/       # MySQLDialect / PostgreSQLDialect / OracleDialect / SQLServerDialect / H2Dialect
   ├─ transaction/   # JdbcTransactionManager / TransactionSynchronizationManager
   ├─ repository/    # JdbcRepository / SimpleJdbcRepository / IdGenerator
   ├─ interceptor/   # SqlInterceptor / LoggingInterceptor / PerformanceInterceptor / TenantInterceptor
   ├─ exception/     # SQLExceptionTranslator / SqlStateClassifier
   ├─ config/        # JdbcProperties / DataSourceConfiguration
   └─ util/          # JdbcUtils / ResultSetExtractor / LobHandler
```

---

## 四、各子包详细设计

#### 4.1 core/ — JDBC 核心模板

##### 定位

JDBC 操作入口，封装连接获取、SQL 执行、结果处理。

```textmate
com.framework.data.jdbc.core
├─ JdbcTemplate              # 核心：query /queryForObject /update /execute
├─ NamedParameterTemplate    # 命名参数（:name）
├─ BatchTemplate             # 批量操作（addBatch /executeBatch）
├─ JdbcOperations            # 操作接口
└─ SqlRunner                 # 原生 SQL执行器
```

#### 4.2 connection/ — 连接池管理

##### 定位

统一连接获取与释放，适配主流连接池，虚拟线程参数调优。

```textmate
com.framework.data.jdbc.connection
├─ ConnectionProvider        # 连接提供者接口
├─ HikariConnectionProvider  # HikariCP 适配（默认）
├─ DruidConnectionProvider   # Druid 适配
├─ SimpleConnectionProvider  # 简易实现（测试用）
├─ ConnectionProxy           # 连接代理（拦截 close/统计）
└─ ConnectionHolder          # 连接持有者（事务内复用）
```

**虚拟线程调优**：

```yaml
framework:
  data:
    datasource:
      hikari:
        maximum-pool-size: 20         # 虚拟线程下 10~30，勿过大
        connection-timeout: 5000      # 5s 快速失败
        leak-detection-threshold: 10000
```

#### 4.3 statement/ — Statement 构建

```textmate
com.framework.data.jdbc.statement
├─ PreparedStatementBuilder   # 预编译语句构建
├─ SqlBuilder                 # SQL 拼接构建器
├─ SqlResult                  # 构建结果（SQL +参数）
├─ ParameterBinder            # 参数绑定
├─ BatchParameterBinder       # 批量参数绑定
└─ SqlParser                  # SQL 解析（方言差异处理）
```

#### 4.4 mapping/ — 结果集映射

```textmate
com.framework.data.jdbc.mapping
├─ BeanPropertyRowMapper<T>   # Bean 属性自动映射
├─ MapRowMapper               # Map 结果
├─ ScalarRowMapper<T>         # 单值结果
├─ DefaultTypeHandlers        # 15+内置类型处理器（含 LocalDateTime）
├─ CamelCaseNamingStrategy    # 驼峰↔下划线
└─ EntityMetadataParser       # 实体元数据解析
```

#### 4.5 dialect/ — 方言实现

```textmate
com.framework.data.jdbc.dialect
├─ AbstractDialect            # 抽象基类（分页模板/函数差异）
├─ MySQLDialect / PostgreSQLDialect
├─ OracleDialect / SQLServerDialect / H2Dialect
├─ JdbcDialectDetector        # 通过 JDBC URL 元数据自动检测
└─ DialectRegistry            # 方言注册表
```

#### 4.6 transaction/ — 事务实现

```textmate
com.framework.data.jdbc.transaction
├─ JdbcTransactionManager     # data-core TransactionManager 实现
├─ JdbcTransaction            # 事务对象
├─ TransactionSynchronizationManager   # 同步管理器（事务内资源绑定）
├─ TransactionSynchronization # 同步回调
└─ IsolationLevelManager      # 隔离级别管理
```

#### 4.7 repository/ — 仓储实现

```textmate
com.framework.data.jdbc.repository
├─ JdbcRepository<T, ID>       # data-core CrudRepository 落地
├─ SimpleJdbcRepository<T, ID> # 简单实现
├─ JdbcRepositoryFactory       # RepositoryFactory 实现（SPI 自动装配）
├─ QueryWrapperTranslator      # QueryWrapper →SQL/参数
└─ IdGenerator                 # AutoIncrement /UUID /Snowflake /Sequence
```

#### 4.8 interceptor/ — SQL 拦截器

```textmate
com.framework.data.jdbc.interceptor
├─ SqlInterceptor              # 拦截器接口
├─ SqlContext                  # 拦截上下文
├─ LoggingInterceptor          # SQL 日志（敏感参数脱敏：委托 coreDesensitizer）
├─ PerformanceInterceptor      # 慢 SQL统计（Micrometer 2.0Timer）
├─ TenantInterceptor           # 多租户条件自动追加
└─ InterceptorChain            # 责任链编排
```

#### 4.9 exception/ + config/ + util/

```textmate
com.framework.data.jdbc.exception
├─ SQLExceptionTranslator      # SQLException →DataAccessException
├─ SqlStateClassifier          # SQLState 分类（模式匹配 switch）
└─ VendorSpecificTranslator    # 厂商特定错误映射

com.framework.data.jdbc.config
├─ JdbcProperties              # 配置属性
├─ DataSourceConfiguration     # 数据源 Bean 配置
└─ RepositoryConfiguration     # 仓储扫描配置

com.framework.data.jdbc.util
├─ JdbcUtils                   # JDBC 工具
├─ ResultSetExtractor<T>       # 结果集提取器
└─ LobHandler                  # LOB 处理
```

---

## 五、核心类关系图

```text
┌────────────────────────────────────────────────────────────────┐
│                 framework-data-jdbc                            │
│                                                                │
│  JdbcTemplate ← NamedParameterTemplate / BatchTemplate         │
│      │                                                         │
│      ├─ ConnectionProvider (Hikari/Druid) ── ConnectionProxy   │
│      ├─ PreparedStatementBuilder ← ParameterBinder             │
│      ├─ RowMapper (BeanProperty) ← TypeHandlers                │
│      ├─ DialectRegistry ← DialectDetector                      │
│      ├─ InterceptorChain (Logging/Performance/Tenant)          │
│      └─ SQLExceptionTranslator ← SqlStateClassifier            │
│                                                                │
│  JdbcRepository ── 实现 ── CrudRepository (data-core)          │
│  JdbcTransactionManager ── 实现 ── TransactionManager          │
│  JdbcRepositoryFactory ── 实现 ── RepositoryFactory (SPI)      │
│                                                                │
│  依赖：framework-data-core · HikariCP/Druid · Jackson 3        │
│  GraalVM：JdbcRuntimeHints 注册反射                            │
└────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0     framework-core
  ▲
L1     framework-data-core（抽象）
  ▲
L2     framework-data-jdbc（本模块实现）
  ▲
L3     framework-boot-autoconfigure（DataSource/Jdbc 自动装配）
```

---

## 七、外部依赖

```xml

<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>tools.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-core</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  data:
    enabled: true
    type: jdbc                          # jdbc / mybatis
    datasource:
      url: jdbc:mysql://localhost:3306/db
      username: root
      password: root
      pool-type: hikari                 # hikari / druid
      hikari:
        maximum-pool-size: 20           # 虚拟线程下 10~30
        connection-timeout: 5000
        leak-detection-threshold: 10000
      router:
        enabled: false                  # 读写分离/多库
    jdbc:
      sql-log-enabled: true             # SQL 日志
      slow-sql-threshold: 500ms
      tenant-enabled: false             # 多租户
```

---

## 九、使用方式

```textmate
// 模板方式
@Autowired
private JdbcTemplate jdbcTemplate;

List<User> users = jdbcTemplate.query(
        "select * from user where status = ?",
        BeanPropertyRowMapper.of(User.class), 1);

// 仓储方式
@Autowired
private CrudRepository<User, Long> userRepository;

userRepository.save(user);

Page<User> page = userRepository.page(PageRequest.of(1, 10),
        new QueryWrapper<User>().eq(User::getStatus, 1));

// 编程式事务
transactionTemplate.execute(status ->{
        jdbcTemplate.update("update account set balance = balance - ? where id = ?",100,1);
    return true;
});
```

---

## 十、SPI 扩展点汇总

| 扩展点接口           | 所在包      | 用途                       |
|:---------------------|:------------|:---------------------------|
| `ConnectionProvider` | connection  | 接入新连接池               |
| `SqlInterceptor`     | interceptor | SQL 拦截（审计/权限/改写） |
| `IdGenerator`        | repository  | 自定义主键生成策略         |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│              framework-boot (适配编排层)                    │
│  └─ DataSourceAutoConfiguration / DataJdbcAutoConfiguration │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-data-jdbc（本模块）                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-data-core（抽象契约）                │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│              framework-core（基础工具）                      │
└──────────────────────────────────────────────────────────────┘
```

**Spring Boot 4.x 适配要点**：虚拟线程下连接池 10~30/5s 快速失败；JsonTypeHandler 用 Jackson 3 不可变 `JsonMapper`；SQL
指标适配 Micrometer 2.0 Timer；`JdbcRuntimeHints` 支持 GraalVM。
