# framework-data-mybatis 模块架构设计

> 文档元信息
> - **模块**：framework-data-mybatis
> - **关键词**：MyBatis Flex、类型安全查询、多表关联、数据脱敏、多租户、SQL 审计
> - **描述**：framework-data-core 的 MyBatis Flex 增强实现，桥接框架自有抽象与 MyBatis Flex，提供企业级数据访问能力
> - **基线**：Spring Boot 4.x + Java 21（MyBatis Flex 1.11+、Jackson 3、GraalVM AOT）

---

## 一、模块定位

`framework-data-mybatis` 是 framework-data-core 的 **MyBatis Flex 增强实现（L2）**，通过适配层桥接框架自有抽象（Repository /
QueryWrapper / TransactionManager / DataSourceRouter / ExceptionTranslator）与 MyBatis Flex
的能力，提供类型安全查询、多表关联、数据脱敏、字段加密、多租户、SQL 审计等企业级功能，同时完全保留 MyBatis 原生 `xxxMapper.xml`
能力。

**核心价值**：

| 场景     | 没有本模块           | 有本模块                                |
|:---------|:---------------------|:----------------------------------------|
| ORM 查询 | 手写 SQL + ResultMap | 类型安全 Lambda 查询 + 多表关联         |
| 仓储抽象 | 直接依赖 MyBatis API | 复用 data-core 仓储契约，可平滑切换实现 |
| 数据安全 | 各业务各自实现       | 脱敏 / 加密 / 多租户 / SQL 审计开箱即用 |
| 代码生成 | 手写 Entity/Mapper   | codegen 生成 Entity/Mapper/TableDef     |

**与核心模块的边界**：

| 模块                       | 定位             | 特点                                   |
|:---------------------------|:-----------------|:---------------------------------------|
| **framework-data-mybatis** | **ORM 增强实现** | **类型安全、企业级能力、保留原生 XML** |
| framework-data-core        | 抽象契约         | 本模块通过 adapter 实现其接口          |
| framework-data-jdbc        | JDBC 轻量实现    | 二选一（或并存，jdbc 处理简单场景）    |

---

## 二、功能清单与依赖矩阵

| 功能         | 子包       | 核心依赖                 | 可选依赖                            |
|:-------------|:-----------|:-------------------------|:------------------------------------|
| 适配桥接     | adapter    | data-core + MyBatis Flex | —                                   |
| 增强仓储     | repository | data-core                | —                                   |
| 查询模型转换 | query      | data-core                | —                                   |
| 企业级扩展   | extension  | MyBatis Flex             | Jackson 3（审计 JSON）              |
| 异常转换     | exception  | data-core                | —                                   |
| 配置与装配   | config     | Spring Boot 4            | spring-boot-configuration-processor |
| 代码生成     | codegen    | MyBatis Flex generator   | FreeMarker                          |

**Spring Boot 4.x 兼容性风险项**：MyBatis Flex ≥ 1.11 需确认已适配 Spring Boot 4 / Spring Framework 7 自动配置机制（
`@AutoConfiguration` 迁移与 `AutoConfiguration.imports` 注册方式）；若 1.11 未完全适配，由本模块 config 子包自建兼容装配作为兜底。

---

## 三、整体包结构

```text
framework-data-mybatis
└─ src/main/java/com/framework/data/mybatis/
   ├─ adapter/        # 核心适配层：FlexRepositoryAdapter / FlexTransactionAdapter / FlexDataSourceAdapter / FlexExceptionTranslator
   ├─ repository/     # 增强仓储：FlexRepository / FlexJoinRepository / FlexDynamicRepository / FlexRepositoryFactory / IdGeneratorAdapter
   ├─ query/          # 查询模型转换：FlexQueryWrapperTranslator / FlexLambdaQueryBuilder / ConditionMapper
   ├─ extension/      # 企业级扩展：FlexAuditHandler / FlexMaskProcessor / FlexEncryptProcessor / FlexTenantHandler / FlexSqlAuditListener / FlexLogicDeleteHandler / ExtensionRegistry
   ├─ exception/      # 异常转换：FlexExceptionConverter / FlexOptimisticLockException
   ├─ config/         # 配置：MybatisFlexAutoConfiguration / MybatisFlexProperties / FlexGlobalConfigCustomizer / MapperScanConfiguration
   └─ codegen/        # 代码生成（可选）：EntityGenerator / MapperGenerator / TableDefGenerator / GeneratorConfig
```

---

## 四、各子包详细设计

#### 4.1 adapter/ — 核心适配层

##### 定位

将 framework-data-core 的抽象契约翻译为 MyBatis Flex 实现，是本模块的桥头堡，也是"核心与实现分离"原则的落地处。

```textmate
cn.jowen.framework.data.mybatis.adapter
├─FlexRepositoryAdapter<T, ID>      #Repository<T, ID> → BaseMapper<T> 适配
│   ├─findById /findList /insert /update /delete
│   └─通过 FlexRepositoryFactory 获取 BaseMapper 后转译调用
├─FlexTransactionAdapter            # TransactionManager 适配 →FlexTransactionManager（Spring 事务）
├─FlexDataSourceAdapter             # DataSourceRouter 适配 →DynamicDataSource（多数据源路由）
└─FlexExceptionTranslator           # ExceptionTranslator 适配 →统一框架异常体系
```

**适配规则**：

- 仓储方法签名 100% 遵循 data-core 的 `Repository<T, ID>` 契约，业务代码不感知 MyBatis；
- 事务语义复用 Spring `PlatformTransactionManager`（Spring Boot 4.x 下 `TransactionManager` 统一前缀）；
- 异常统一翻译为 data-core exception 子包定义的框架异常，`@Transactional` 回滚规则不变。

#### 4.2 repository/ — 增强仓储

##### 定位

面向业务的核心入口，在 data-core 契约之上叠加 MyBatis Flex 专属能力。

```textmate
cn.jowen.framework.data.mybatis.repository
├─FlexRepository<T, ID>             #完整功能仓储（CRUD +分页 +条件 +批量）
├─FlexJoinRepository<T>             #多表关联查询仓储（Flex QueryWrapper join）
├─FlexDynamicRepository             #无实体动态仓储（Map/JSON 数据源）
├─FlexRepositoryFactory             #仓储工厂：注入 BaseMapper、注册自定义扩展
└─IdGeneratorAdapter                #主键生成适配（雪花 /自增 /自定义 SPI）
```

**使用示意**：

```textmate
// 声明式接口继承即可获得全部能力
public interface UserRepository extends FlexRepository<User, Long> {
    // 可追加自定义方法（配合 @Select / XML）
}

// 注入使用
@Autowired
private UserRepository userRepository;

List<User> users = userRepository.selectList(
    FlexLambdaQueryBuilder.create(User.class)
        .eq(User::getStatus, 1)
        .orderByDesc(User::getCreateTime));
```

#### 4.3 query/ — 查询模型转换

##### 定位

把 data-core 的 `QueryWrapper`（无 Spring、无 ORM 依赖的纯抽象）翻译为 MyBatis Flex 的 `QueryWrapper`，并原生暴露 Lambda
类型安全查询。

```textmate
cn.jowen.framework.data.mybatis.query
├─FlexQueryWrapperTranslator        #data-core QueryWrapper → Flex QueryWrapper
│   ├─条件转换：eq/ne/gt/ge/lt/le/in/between/like
│   ├─排序转换：orderBy/orderByDesc
│   ├─分页转换：limit/offset →Page
│   └─联表转换：join 条件 → Flex join 模型
├─FlexLambdaQueryBuilder            # Lambda 类型安全查询入口（静态工厂）
└─ConditionMapper                   # 条件操作符映射（框架枚举 ↔ Flex 枚举）
```

**设计要点**：翻译器保持纯函数、无状态，单测覆盖每种操作符的往返一致性；业务层直接使用 `FlexLambdaQueryBuilder` 时绕过翻译器，降低开销。

#### 4.4 extension/ — 企业级扩展

##### 定位

通过 MyBatis Flex 的拦截器/处理器机制实现横切能力，全部可独立开关。

```textmate
cn.jowen.framework.data.mybatis.extension
├─FlexAuditHandler               #数据审计：记录 insert / update 前后快照（审计表落库）
├─FlexMaskProcessor              #数据脱敏：查询结果按注解脱敏（@Mask委托 core.Desensitizer）
├─FlexEncryptProcessor           #字段加密：写入加密、读取解密（@Encrypt 字段级）
├─FlexTenantHandler              #多租户：自动追加 tenant_id 条件（ContextCarrier 驱动）
├─FlexSqlAuditListener           #SQL 审计日志：慢 SQL /全量 SQL 输出（SLF4J +Micrometer 2.0）
├─FlexLogicDeleteHandler         #逻辑删除增强：@LogicDelete 字段自动改写
└─ExtensionRegistry              #扩展注册中心：统一注册/排序/去重
```

**上下文统一（冲突修正决议 #3）**：多租户上下文、脱敏跳过上下文、数据权限上下文统一读写 `core.context.ContextCarrier`（默认
ScopedValue，`framework.context.mode=threadlocal` 兼容切换）， **不再各自实现**；跨虚拟线程迁移用
`ContextSnapshot.capture()/replay()`。

**脱敏统一（冲突修正决议 #1）**：`FlexMaskProcessor` 只做"结果集字段扫描 + 委托执行"，规则与策略全部来自 `core.desensitize`（
`@Mask(strategy=...)` 解析为 `core.desensitize.DesensitizeField`，执行走 `Desensitizer`）；自定义策略实现 `DesensitizeRule`
一处注册，日志/结果集/JSON 输出处处生效。

```textmate
// FlexAuditHandler 的 JSON 快照序列化 → Jackson 3（tools.jackson）
public class FlexAuditHandler {
    private final tools.jackson.databind.json.JsonMapper jsonMapper =
            tools.jackson.databind.json.JsonMapper.builder().build();
}
```

#### 4.5 exception/ — 异常转换

##### 定位

MyBatis 异常 → 框架统一异常体系。

```textmate
cn.jowen.framework.data.mybatis.exception
├─FlexExceptionConverter            #MyBatisExceptionTranslator：PersistenceException →DataAccessException
└─FlexOptimisticLockException       #乐观锁冲突异常（@Version 字段更新影响行数为 0时抛出）
```

**转换规则**：优先复用 data-core exception 的既有分类（连接/约束/唯一冲突/乐观锁）；MyBatis 特有异常（如 SQL 语法）归入
`DataAccessResourceFailure` 或 `InvalidDataAccessResourceUsageException`。

#### 4.6 config/ — 配置与装配

##### 定位

Spring Boot 4 自动装配 + 配置属性绑定 + Mapper 扫描。

```textmate
cn.jowen.framework.data.mybatis.config
├─MybatisFlexAutoConfiguration       #自动装配：SqlSessionFactory /FlexGlobalConfig /拦截器链
├─MybatisFlexProperties              #@ConfigurationProperties(prefix = "framework.data.mybatis")
├─FlexGlobalConfigCustomizer         #全局配置定制回调（用户可覆写）
└─MapperScanConfiguration            #@MapperScan 兼容注册（含 MapperFactoryBean 定制）
```

**注册文件**：

```text
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
cn.jowen.framework.data.mybatis.config.MybatisFlexAutoConfiguration
```

**Spring Boot 4.x 适配要点**：

1. **版本要求**：MyBatis Flex ≥ 1.11（需支持 Spring Boot 4 自动配置）；若官方未就绪，本模块提供兼容装配兜底；
2. **GraalVM AOT**：实现 `RuntimeHintsRegistrar`，注册 Mapper 接口反射元数据与 XML 资源；
3. **连接池调优**：虚拟线程下 `maximumPoolSize` 10~30、`connectionTimeout` 5s（与 data-jdbc 同策略）；
4. **Jackson 3**：审计/脱敏/加密处理器统一使用 `tools.jackson` 不可变 `JsonMapper`。

```textmate
// GraalVM 原生镜像支持
public class MybatisRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerType(BaseMapper.class, MemberCategory.values());
        hints.resources()
                .registerPattern("mapper/**/*Mapper.xml");
    }
}
```

#### 4.7 codegen/ — 代码生成（可选）

##### 定位

基于表结构生成 Entity / Mapper / TableDef，减少样板代码。

```textmate
cn.jowen.framework.data.mybatis.codegen
├─EntityGenerator          #生成实体类（JSpecify 空注解、@Table 元数据）
├─MapperGenerator          #生成 Mapper 接口（继承 FlexBaseMapper）
├─TableDefGenerator        #生成 TableDef（Lambda 类型安全引用）
└─GeneratorConfig          #生成配置（表名/包名/命名策略/输出目录）
```

**设计要点**：codegen 仅作为独立入口暴露，不参与运行时装配；生成产物可二次人工修改，不做强制覆盖。

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────────────────────────────┐
│                    framework-data-mybatis                       │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  config（装配层）                         │  │
│  │  MybatisFlexAutoConfiguration ──→ MybatisFlexProperties   │  │
│  │  ├─ SqlSessionFactory（Spring Boot 4.x 适配）             │  │
│  │  ├─ FlexGlobalConfig（拦截器链注册）                      │  │
│  │  ├─ RuntimeHints（GraalVM AOT）                           │  │
│  │  └─ MapperScanConfiguration（@MapperScan）                │  │
│  └─────────────────────────┬─────────────────────────────────┘  │
│                            │ 注入                               │
│  ┌─────────────────────────▼─────────────────────────────────┐  │
│  │                 repository（仓储层）                      │  │
│  │  FlexRepositoryFactory ──→ FlexRepository / FlexJoinRepo  │  │
│  │                        └──→ FlexDynamicRepository         │  │
│  └─────────────────────────┬─────────────────────────────────┘  │
│                            │ 桥接                               │
│  ┌─────────────────────────▼─────────────────────────────────┐  │
│  │                 adapter（适配层）                         │  │
│  │  FlexRepositoryAdapter ──→ BaseMapper                     │  │
│  │  FlexTransactionAdapter ──→ TransactionManager            │  │
│  │  FlexDataSourceAdapter  ──→ DataSourceRouter              │  │
│  │  FlexExceptionTranslator ──→ 框架异常体系                 │  │
│  └─────────────────────────┬─────────────────────────────────┘  │
│                            │ 拦截                               │
│  ┌─────────────────────────▼─────────────────────────────────┐  │
│  │              extension（横切扩展链）                      │  │
│  │  FlexAuditHandler → FlexMaskProcessor → FlexEncrypt       │  │
│  │  → FlexTenantHandler → FlexSqlAuditListener               │  │
│  │  → FlexLogicDeleteHandler → ExtensionRegistry             │  │
│  └─────────────────────────┬─────────────────────────────────┘  │
│                            │                                    │
│  ┌─────────────────────────▼─────────────────────────────────┐  │
│  │          framework-data-core（抽象契约，零 Spring）       │  │
│  │  Repository / QueryWrapper / TransactionManager / Router  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0（零依赖）        framework-data-core（抽象契约）
                      ▲
L1（依赖 L0）        framework-data-mybatis 内部：adapter → repository → query
                      ▲
L2（依赖 L1）        extension / exception（横切扩展）
                      ▲
L3（依赖 L0~L2）     config（自动装配，唯一依赖 Spring Boot 的层）
                      ▲
L4（独立）           codegen（可选，不参与运行时）
```

**模块间规则**：

- 本模块依赖 `framework-data-core`（compile）、`MyBatis Flex`（compile）；
- 仅 `config` 子包依赖 Spring Boot 自动装配机制，其余子包保持框架抽象中立；
- 与 `framework-data-jdbc` 互斥或并存：同一 `DataSource` 可被两实现共用（jdbc 处理简单场景、mybatis 处理复杂场景），事务管理器共用一份。

---

## 七、外部依赖

```xml

<dependencies>
    <!-- 框架内依赖 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-core</artifactId>
    </dependency>

    <!-- MyBatis Flex（核心 ORM） -->
    <dependency>
        <groupId>com.mybatis-flex</groupId>
        <artifactId>mybatis-flex-core</artifactId>
        <version>1.11.0</version>
    </dependency>
    <dependency>
        <groupId>com.mybatis-flex</groupId>
        <artifactId>mybatis-flex-spring-boot4-starter</artifactId>
        <version>1.11.0</version>
    </dependency>

    <!-- Spring Boot 自动装配（仅 config 子包使用） -->
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

    <!-- Jackson 3（审计/脱敏 JSON，可选） -->
    <dependency>
        <groupId>tools.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 代码生成（可选） -->
    <dependency>
        <groupId>com.mybatis-flex</groupId>
        <artifactId>mybatis-flex-codegen</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  data:
    type: mybatis                            # jdbc / mybatis（二选一）
    datasource:
      url: jdbc:mysql://localhost:3306/db
      username: root
      password: root
      hikari:
        maximum-pool-size: 20                # 虚拟线程下 10~30
        connection-timeout: 5000             # 5s 快速失败
    mybatis:
      enabled: true
      mapper-locations: classpath*:/mapper/**/*Mapper.xml   # 原生 XML 支持
      type-aliases-package: com.example.domain
      audit-enabled: true                    # 数据审计
      encrypt-enabled: true                  # 字段加密
      tenant-enabled: true                   # 多租户
      sql-audit-enabled: true                # SQL 审计日志
      logic-delete-enabled: true             # 逻辑删除
      optimistic-lock-enabled: true          # 乐观锁（@Version）
    desensitize:
      enabled: true                          # 脱敏全局开关（core 统一）
    context:
      mode: scopedvalue                      # scopedvalue / threadlocal（虚拟线程推荐前者，core 统一）
```

---

## 九、使用方式

#### 9.1 引入依赖

```xml
<!-- 基础 Starter -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-boot-starter</artifactId>
</dependency>

<!-- 数据访问实现：MyBatis -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-data-mybatis</artifactId>
</dependency>
```

#### 9.2 定义实体与仓储

```textmate

@Table("user")
public class User {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("username")
    private String username;

    @Mask(strategy = MaskStrategy.PHONE)   // 语义等价 core @DesensitizeField(strategy=PHONE)，执行委托 core Desensitizer
    private String phone;          // 查询结果自动脱敏

    @Encrypt
    private String idCard;         // 写入加密、读取解密
}

public interface UserRepository extends FlexRepository<User, Long> {
}
```

#### 9.3 业务使用

```textmate

@Service
public class UserService {
    private final UserRepository userRepository;

    public Page<User> page(int pageNo, int pageSize) {
        return userRepository.paginate(
                FlexLambdaQueryBuilder.create(User.class)
                        .eq(User::getStatus, 1),
                pageNo, pageSize);
    }

    public List<User> joinDept() {
        // 多表关联
        return joinRepository.selectJoinList(
                FlexLambdaQueryBuilder.create(User.class)
                        .innerJoin(Dept.class).on(User::getDeptId, Dept::getId));
    }
}
```

#### 9.4 原生 XML 共存

```xml
<!-- src/main/resources/mapper/UserExtMapper.xml -->
<select id="selectCustomStats" resultType="java.util.Map">
    SELECT dept_id, COUNT(*) cnt FROM user GROUP BY dept_id
</select>
```

---

## 十、SPI 扩展点汇总

| 扩展点                       | 所在包     | 用途                                                                 |
|:-----------------------------|:-----------|:---------------------------------------------------------------------|
| `FlexRepositoryFactory`      | repository | 自定义仓储工厂（注入扩展处理器）                                     |
| `IdGeneratorAdapter`         | repository | 自定义主键生成策略                                                   |
| `FlexAuditHandler`           | extension  | 自定义审计快照处理                                                   |
| `FlexMaskProcessor`          | extension  | 结果集脱敏适配（规则用 core `DesensitizeRule` SPI 扩展，不在此定义） |
| `FlexEncryptProcessor`       | extension  | 自定义加解密算法                                                     |
| `FlexTenantHandler`          | extension  | 自定义租户上下文解析                                                 |
| `FlexSqlAuditListener`       | extension  | 自定义 SQL 审计输出                                                  |
| `FlexExceptionConverter`     | exception  | 自定义异常翻译规则                                                   |
| `FlexGlobalConfigCustomizer` | config     | 自定义 MyBatis Flex 全局配置                                         |
| `ExtensionRegistry`          | extension  | 扩展注册顺序/去重策略                                                |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│                    业务应用（Application）                  │
└──────────────────────────┬──────────────────────────────────┘
                           │ 使用 FlexRepository / FlexLambdaQueryBuilder
┌──────────────────────────▼──────────────────────────────────┐
│              framework-data-mybatis（ORM 增强实现）         │
│  adapter 桥接 data-core 契约 → MyBatis Flex                 │
│  extension 提供 脱敏/加密/多租户/审计/逻辑删除/乐观锁       │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
┌──────────────▼──────────────┐  ┌────────────▼───────────────┐
│  framework-data-core（抽象）│  │  MyBatis Flex 1.11+        │
│  Repository / QueryWrapper  │  │  BaseMapper / QueryWrapper │
│  Transaction / Router / ... │  │  Interceptor / 代码生成    │
└──────────────┬──────────────┘  └────────────┬───────────────┘
               │                              │
┌──────────────▼──────────────────────────────▼───────────────┐
│  Spring Boot 4.x（@AutoConfiguration / 事务 / AOT）         │
│  + 连接池（HikariCP） + Jackson 3 + Micrometer 2.0          │
└─────────────────────────────────────────────────────────────┘
```

**依赖方向**：业务 → mybatis → data-core（契约）→ Spring Boot（装配/事务）。业务代码只感知 data-core 契约与本模块仓储 API，不感知
MyBatis 内部细节，为未来实现替换（如 JPA 适配）保留余地。
