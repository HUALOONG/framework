# framework-data-core 模块架构设计

> 文档元信息
> - **模块**：framework-data-core
> - **关键词**：数据访问抽象、仓储、查询、分页、事务、方言、零 Spring 依赖
> - **描述**：数据访问层纯抽象层，定义仓储/查询/分页/事务/数据源/映射/方言/异常抽象； **零 JDBC 依赖、零 Spring 依赖**
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-data-core` 是框架的 **数据访问抽象层（L1）**，定义与实现无关的数据访问契约：仓储接口、查询模型、分页排序、事务抽象、数据源抽象、对象映射、数据库方言、异常体系与实体基类。
**本模块零 JDBC、零 Spring 依赖**，由 jdbc / mybatis 实现层落地，autoconfigure 层桥接 Spring。

**核心价值**：

| 场景             | 没有本模块                     | 有本模块                               |
| :--------------- | :----------------------------- | :------------------------------------- |
| 数据访问实现切换 | 业务代码绑死 JDBC/MyBatis API  | 面向抽象编程，实现层可替换             |
| 单元测试         | 需要 Spring 容器/Database 环境 | 纯抽象可 mock，脱离容器测试            |
| 多数据库         | 每个库方言硬编码               | Dialect SPI + DialectDetector 自动识别 |
| 版本升级         | 升级实现库牵连全部业务         | 实现层替换，业务零改动                 |

**与核心模块的边界**：

| 模块                    | 定位             | 特点                               |
| :---------------------- | :--------------- | :--------------------------------- |
| **framework-data-core** | **数据访问抽象** | **零实现、零 Spring**              |
| framework-data-jdbc     | JDBC 实现        | 依赖 data-core，提供轻量 JDBC 落地 |
| framework-data-mybatis  | MyBatis 实现     | 依赖 data-core，桥接 MyBatis Flex  |
| framework-core          | 基础设施         | SPI / 断言 / 异常                  |

---

## 二、功能清单与依赖矩阵

| 功能       | 子包        | 核心依赖       | 可选依赖 |
| :--------- | :---------- | :------------- | :------- |
| 仓储抽象   | repository  | framework-core | —        |
| 查询模型   | query       | —              | —        |
| 分页模型   | page / sort | —              | —        |
| 事务抽象   | transaction | —              | —        |
| 数据源抽象 | datasource  | —              | —        |
| 对象映射   | mapping     | —              | —        |
| 数据库方言 | dialect     | —              | —        |
| 回调钩子   | callback    | —              | —        |
| 异常体系   | exception   | framework-core | —        |
| 实体基类   | support     | —              | —        |

---

## 三、整体包结构

```text
framework-data-core
└─ src/main/java/cn/jowen/framework/data/core/
   ├─ callback        # 回调
   ├─ datasource/     # 数据源抽象（DataSourceRouter / PoolType）
   ├─ dialect/        # 方言（DatabaseDialect SPI / DatabaseType）
   ├─ exception/      # 异常体系（DataAccessException 树 + ExceptionTranslator SPI）
   ├─ mapping/        # 对象映射（EntityMetadata / RowMapper SPI / TypeHandler）
   ├─ page/           # 分页（Page / PageRequest）
   ├─ query/          # 查询模型（QueryWrapper / Condition / Operator）
   ├─ repository/     # 仓储抽象（Repository / CrudRepository / PagingRepository）
   ├─ sort/           # 排序（Sort / Order / Direction）
   ├─ support/        # 实体基类（EntityBase / VersionedEntity / AuditableEntity）
   └─ transaction/    # 事务抽象（TransactionManager SPI / TransactionTemplate）
```

---

## 四、各子包详细设计

#### 4.1 repository/ — 仓储抽象

##### 定位

面向业务的数据访问契约，实现层（jdbc/mybatis）各自落地。

```textmate
cn.jowen.framework.data.core.repository
├─ CrudRepository<T, ID>    # CRUD：save / findById / deleteById...
├─ DynamicRepository        # 动态仓储（无实体类）
├─ PagingRepository<T, ID>  # 分页：page(PageRequest, QueryWrapper)
├─ Repository<T, ID>        # 顶层接口
└─ RepositoryFactory        # 仓储工厂（SPI）
```

> **禁用约束（冲突修正决议 # 4）**：`Repository / CrudRepository / Page` 等与 Spring Data 命名撞名—— **禁止与 `spring-data-*` 同用**（import 歧义、AOP 仓储代理互伤）；需 Spring Data 生态时二选一，不得混装。实现层（data-jdbc/data-mybatis）同样遵守。

#### 4.2 query/ — 查询模型

##### 定位

类型安全的条件构建器，与实现无关。

```textmate
cn.jowen.framework.data.core.query
├─ Condition                # 单个条件（column/op/value）
├─ JoinType                 # 连接类型（INNER/LEFT/RIGHT）
├─ Operator                 # 操作符枚举（EQ/NE/GT/LT/IN/LIKE...）
├─ QueryWrapper<T>          # 查询条件：eq / like / in / between / orderBy
└─ UpdateWrapper<T>         # 更新条件
```

#### 4.3 page/ + sort/ — 分页与排序

```textmate
cn.jowen.framework.data.core.page
├─ Page<T>                  # 分页结果（records / total / pageNo / pageSize）
├─ Pageable                 # 分页能力标记
└─ PageRequest              # 分页请求

cn.jowen.framework.data.core.sort
├─ Sort / Order             # 排序定义
├─ Direction                # ASC / DESC
└─ NullHandling             # NULLS_FIRST / NULLS_LAST
```

#### 4.4 transaction/ — 事务抽象

##### 定位

事务管理器 SPI，屏蔽 JDBC/MyBatis 事务差异。

```textmate
cn.jowen.framework.data.core.transaction
├─ Isolation                # 隔离级别（DEFAULT/READ_COMMITTED...）
├─ Propagation              # 传播行为（REQUIRED/REQUIRES_NEW/NESTED...）
├─ TransactionCallback<T>   # 事务回调
├─ TransactionDefinition    # 事务定义（传播/隔离/超时/只读）
├─ TransactionManager       # 事务管理器（SPI）：begin / commit / rollback
├─ TransactionStatus        # 事务状态
└─ TransactionTemplate      # 编程式事务模板
```

**Spring 场景约定**：`TransactionManager` 定位为 **实现适配接口**，不定义新的事务语义。Spring 场景下：

- 语义（传播/隔离）对齐 spring-tx 的 `Propagation/Isolation`，数值一一对应；
- 实现层提供 **spring-tx 薄适配**（`SpringTransactionManagerAdapter implements TransactionManager`，内部委托
  `PlatformTransactionManager`），保证 `@Transactional` 与 `TransactionTemplate` 共享同一事务同步器；
- 纯 JDBC 场景（无 Spring）用自研 `JdbcTransactionManager`，两实现互斥选择，不共存。

#### 4.5 datasource/ — 数据源抽象

```textmate
cn.jowen.framework.data.core.datasource
├─ DataSource               # 数据源接口
├─ DataSourceContext        # 数据源路由选择（基于 core ContextCarrier，虚拟线程友好）
├─ DataSourceProperties     # 配置属性
├─ DataSourceRouter         # 动态数据源路由（读写分离/多库）
└─ PoolType                 # 连接池类型（HIKARI /DRUID）
```

**上下文统一**：`DataSourceContext` 的当前路由读写委托 `core.context.ContextCarrier`（默认 ScopedValue，
`framework.context.mode=threadlocal` 兼容），与 i18n/多租户/trace 共用同一载体；跨虚拟线程迁移用 `ContextSnapshot`。

#### 4.6 mapping/ — 对象映射

```textmate
cn.jowen.framework.data.core.mapping
├─ EntityMetadata<T>        # 实体元数据（Record 承载）
├─ EntityScanner            # 实体扫描器（SPI）
├─ JdbcType                 # JDBC 类型枚举
├─ NamingStrategy           # 命名策略（驼峰↔下划线）
├─ PropertyMetadata         # 属性元数据
├─ RowMapper<T>             # 行映射器（SPI）
├─ TypeHandler<T>           # 类型处理器
└─ TypeHandlerRegistry      # 类型处理器注册中心
```

#### 4.7 dialect/ — 数据库方言

```textmate
cn.jowen.framework.data.core.dialect
├─ DatabaseDialect          # 方言接口（SPI）：分页 SQL /函数 /关键字转义
├─ DatabaseType             # MYSQL /POSTGRESQL /ORACLE /SQLSERVER /H2
└─ DialectDetector          # 方言检测器（SPI）
```

#### 4.8 callback/ — 回调钩子

```textmate
cn.jowen.framework.data.core.callback
├─ ConnectionCallback<T>    # 连接回调
├─ EntityCallback           # 实体生命周期回调（插入前/更新前）
└─ StatementCallback<T>     # Statement 回调
```

#### 4.9 exception/ — 数据访问异常体系

```textmate
cn.jowen.framework.data.core.exception
├─ BadSqlGrammarException               # SQL 语法错误
├─ DataAccessException                  # 异常根类
├─ DataIntegrityViolationException      # 数据完整性违例
├─ DeadlockException                    # 死锁
├─ DuplicateKeyException                # 唯一键冲突
├─ ExceptionTranslator                  # 异常转换器（SPI）
├─ OptimisticLockException              # 乐观锁失败
├─ TimeoutException                     # 超时
└─ TransientDataAccessException         # 可重试异常
```

#### 4.10 support/ — 实体基类

```textmate
cn.jowen.framework.data.core.support
├─ AuditableEntity<ID>      # 审计基类（createBy/createTime/updateBy/updateTime）
├─ EntityBase<ID>           # 主键基类
└─ VersionedEntity<ID>      # 乐观锁版本基类（@Version）
```

---

## 五、核心类关系图

```text
┌────────────────────────────────────────────────────────────────────┐
│                  framework-data-core                               │
│                                                                    │
│  业务 ──→ Repository (Crud/Paging/Dynamic)                         │
│              │ 依赖                                                │
│              ▼                                                     │
│  QueryWrapper ← Condition/Operator ──→ RepositoryFactory(SPI)      │
│              │                                                     │
│  TransactionTemplate ── TransactionManager(SPI)                    │
│  DataSourceRouter ── DataSourceContext ── PoolType                 │
│  EntityMetadata ← NamingStrategy/TypeHandler ← EntityScanner(SPI)  │
│  DatabaseDialect(SPI) ← DialectDetector                            │
│  DataAccessException ← ExceptionTranslator(SPI)                    │
│  EntityBase / VersionedEntity / AuditableEntity                    │
│                                                                    │
│  ⚠ 零 JDBC 依赖 · 零 Spring 依赖 · 仅 framework-core              │
└────────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0     framework-core
  ▲
L1     framework-data-core（本模块，纯抽象）
  ▲
L2     framework-data-jdbc / framework-data-mybatis（实现）
  ▲
L3     framework-boot-autoconfigure（桥接 Spring）
```

---

## 七、外部依赖

```xml

<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>
    <!-- 无 JDBC、无 Spring：仅此一个框架内依赖 -->
</dependencies>
```

---

## 八、配置属性

无（纯抽象模块，配置由实现层 jdbc/mybatis 提供）。

---

## 九、使用方式

```textmate
// 面向抽象编程（实现层注入）
@Autowired
private CrudRepository<User, Long> userRepository;

// 类型安全查询
QueryWrapper<User> qw = new QueryWrapper<User>()
        .eq(User::getStatus, 1)
        .like(User::getUsername, "张")
        .orderByDesc(User::getCreateTime);

Page<User> page = userRepository.page(PageRequest.of(1, 10), qw);

// 编程式事务
transactionTemplate.execute(status ->{
    userRepository.save(user);
    return true;
});
```

---

## 十、SPI 扩展点汇总

| 扩展点接口                            | 所在包      | 用途             |
| :------------------------------------ | :---------- | :--------------- |
| `RepositoryFactory`                   | repository  | 自定义仓储工厂   |
| `TransactionManager`                  | transaction | 自定义事务实现   |
| `RowMapper`                           | mapping     | 自定义结果集映射 |
| `EntityScanner`                       | mapping     | 自定义实体扫描   |
| `DatabaseDialect` / `DialectDetector` | dialect     | 新数据库方言     |
| `ExceptionTranslator`                 | exception   | 自定义异常翻译   |

---

## 十一、与整体框架的关系

```text
┌──────────────────────────────────────────────────────────────┐
│  framework-boot-autoconfigure（适配编排层）                  │
│  └─ data/ 子包：DataSourceAutoConfiguration                  │
│                 DataJdbcAutoConfiguration                    │
│                 DataMybatisAutoConfiguration                 │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│  framework-data-jdbc       framework-data-mybatis            │
│  （JDBC 轻量实现）          （MyBatis Flex 桥接）            │
└──────────────────────────┬───────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────┐
│            framework-data-core（本模块，纯抽象）             │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│              framework-core（SPI / 断言 / 异常）             │
└──────────────────────────────────────────────────────────────┘
```

**Java 21 适配**：EntityMetadata 用 Record 承载；包级 `@NullMarked`；异常分类用模式匹配 switch。
