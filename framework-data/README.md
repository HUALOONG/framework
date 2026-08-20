# framework-data 模块架构设计

> 文档元信息
> - **模块**：framework-data
> - **关键词**：数据访问聚合、POM 聚合、零代码、统一入口、实现二选一
> - **描述**：数据访问聚合父模块，仅含 pom.xml 无代码，聚合 data-core / data-jdbc / data-mybatis 三个子模块
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-data` 是数据访问领域的 **聚合父模块（纯 POM，零代码）**，聚合三个数据子模块：

```text
framework-data（聚合父模块，纯 POM）
├─ framework-data-core       # 数据访问抽象（repository/query/transaction/...，零 Spring）
├─ framework-data-jdbc       # JDBC 轻量实现（data-core 的 JDBC 落地）
└─ framework-data-mybatis    # MyBatis Flex 增强实现（data-core 的 ORM 落地）
```

**核心职责**：

1. **统一数据入口**：业务方只需引入 `framework-data` 一个依赖，即获得抽象 + 默认实现，无需分别协调
   data-core/data-jdbc/data-mybatis 的版本与依赖关系；
2. **实现二选一的承载点**：jdbc 与 mybatis 是两个互斥实现，聚合器负责把"选择权"交给业务方（见 §四/§六），自身不替业务方做决定；
3. **版本后置**：聚合器内不写版本号，全部由 `framework-bom`（import scope）与根 pom 的 dependencyManagement 统一裁决。

**与相邻模块的边界**：

| 模块                           | 分工                                               |
|:-------------------------------|:---------------------------------------------------|
| `framework-bom`                | 管版本（框架模块版本字典），不聚合实现             |
| `framework-boot-starter`       | 管"启动"（Boot 运行时 + 装配层），默认不含数据访问 |
| **`framework-data`（本模块）** | 管"数据访问域"聚合，与 Boot 无关，纯 POM           |
| `framework-boot`               | 超级聚合器，管"全仓库构建"，是构建期概念           |

**核心价值**：

| 场景         | 没有本模块                                         | 有本模块                           |
|:-------------|:---------------------------------------------------|:-----------------------------------|
| 引入数据能力 | 手动引 3 个依赖（core 抽象 + jdbc/mybatis + 版本） | 一个依赖 `framework-data`          |
| 实现切换     | 改依赖坐标，易漏改                                 | 聚合器内一处注释说明，按需排他即可 |
| 依赖遗漏     | 漏引 data-core 导致抽象类缺失                      | 聚合保证抽象始终在                 |

> **注意**：`framework-data` 是 **可选聚合入口**，与 `framework-boot-starter` 平级；业务方也可跳过本模块、直接引入
> data-core + 具体实现（自由度等价，本模块只省去"多写两个依赖"）。

---

## 二、功能清单与依赖矩阵

| 功能              | 形式     | 依赖                                  | 说明                                                     |
|:------------------|:---------|:--------------------------------------|:---------------------------------------------------------|
| 数据访问抽象      | 传递依赖 | framework-data-core                   | repository/query/page/transaction 等 11 子包             |
| JDBC 轻量实现     | 传递依赖 | framework-data-jdbc                   | 默认实现（HikariCP/Druid + JdbcTemplate 风格）           |
| MyBatis Flex 实现 | 传递依赖 | framework-data-mybatis                | 备选实现（Flex ≥1.11，Spring Boot 4.x 兼容风险项见 §七） |
| 连接池            | 传递依赖 | HikariCP（经 data-jdbc）              | 虚拟线程调优参数见 data-jdbc 文档                        |
| SQL 能力          | 传递依赖 | MyBatis Flex（经 data-mybatis，按需） | 不与 data-jdbc 同时启用                                  |

**聚合策略**：

- **默认**：data-core + data-jdbc 生效（无 MyBatis Flex 依赖时不装配 mybatis 能力，靠 `@ConditionalOnClass` 隔离）；
- **二选一**：引入 mybatis 实现时，业务方应排除 data-jdbc 或仅在 mybatis 体系下使用（两者可并存于 classpath，但会形成两套数据访问体系，
  **不推荐同时使用**，见 §六）。

---

## 三、模块结构

```text
framework-data
├─ pom.xml                          # 唯一内容：聚合声明
└─ src/
   └─ (无 main/java / resources —— 零代码模块)
```

> 与 framework-bom / framework-boot-starter / framework-boot 相同，聚合器一律零代码，避免重复初始化与类冲突。

---

## 四、pom.xml 详细设计

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-parent</artifactId>   <!-- 根 pom：统一 properties + dependencyManagement -->
        <version>4.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>framework-data</artifactId>
    <packaging>pom</packaging>
    <name>framework-data</name>
    <description>Framework Data - 数据访问聚合父模块（零代码）</description>

    <dependencies>
        <!-- 抽象层：必须存在 -->
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-data-core</artifactId>
        </dependency>

        <!-- 默认实现：JDBC 轻量 -->
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-data-jdbc</artifactId>
        </dependency>

        <!-- 备选实现：MyBatis Flex（默认不传递，业务方按需显式引入） -->
        <!--
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-data-mybatis</artifactId>
        </dependency>
        -->
    </dependencies>
</project>
```

**设计取舍（与整体方案 §8.13"聚合三个子模块"的一致性说明）**：

整体方案原文为"聚合 data-core / data-jdbc / data-mybatis 三个子模块"，本设计在此之上增加一条 **实现选择规则**：

- `data-mybatis` 以 **注释形式**列在聚合器中作为"备选实现入口"（业务方取消注释即可切换），而非无条件传递；
- 理由：若两者无条件同时传递，业务方 classpath 同时存在两套实现，`boot-autoconfigure` 的 `FrameworkDataAutoConfiguration`
  将按 `@ConditionalOnClass` 各自装配，形成 **双数据访问体系并存**——这在功能上合法（无冲突装配），但语义上违背"jdbc/mybatis
  二选一"的模块设计意图，且引入无用依赖；
- 若希望聚合器内 **全量传递**、由业务方自行排他，只需把注释块启用并在使用时按 §六 的排他说明操作即可（两种用法均被支持）。

---

## 五、核心类关系图

```text
┌────────────────────────────────────────────────────────────────┐
│             framework-data（纯 POM 聚合父模块）                │
│                                                                │
│  ┌───────────────────────────────────────────────────────┐     │
│  │ framework-data-core（抽象，零 Spring）                │     │
│  │  Repository<T,ID> / QueryRepository / Page            │     │
│  │  TransactionManager / DataSourceRouter / Dialect      │     │
│  └──────────────────────────┬────────────────────────────┘     │
│                             │ 实现                             │
│      ┌──────────────────────┼──────────────────────┐           │
│      ▼                      ▼                      ▼           │
│  framework-data-jdbc   framework-data-mybatis   (业务方)       │
│  JdbcTemplate 风格     Flex + 脱敏/加密/多租户  自定义实现     │
│  连接池调优 10~30      审计/乐观锁/逻辑删除     (Repository    │
│  JsonTypeHandler       Jackson 3 + AOT           Factory)      │
│                                                                │
│  装配入口（运行时由 boot-autoconfigure 按 @ConditionalOnClass  │
│  择一装配）：FrameworkDataAutoConfiguration                    │
└────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L1（抽象层）        framework-data-core（零 Spring）
                      ▲
L2（实现层）         framework-data-jdbc / framework-data-mybatis（二选一）
                      ▲
L3（装配层）         framework-boot-autoconfigure（FrameworkDataAutoConfiguration）
                      ▲
L4（聚合层）         ★ framework-data（本模块，纯 POM，可选入口）
```

**模块间规则**：

1. **抽象→实现方向不可逆**：data-core 不依赖任何实现；jdbc/mybatis 只依赖 data-core，互不依赖；
2. **jdbc/mybatis 二选一**：二者功能域重叠（都是 data-core 的落地）， **禁止业务方同时启用两套数据访问体系**；若 classpath
   同时存在，以 `framework.data.implementation`（见 §八）显式声明为准，缺省时按"先出现者优先 + 日志告警"处理；
3. **聚合器无运行时职责**：framework-data 的依赖全部为 compile（传递），但自身不触发任何 Bean 注册——装配决策全部后置给
   autoconfigure 的条件注解；
4. **与 starter 正交**：`framework-boot-starter` 默认 **不**依赖 framework-data（保持默认轻量），业务方需要数据能力时自行追加本模块依赖。

---

## 七、外部依赖

| 依赖                     | scope                         | 用途     |
|:-------------------------|:------------------------------|:---------|
| `framework-data-core`    | compile（传递）               | 抽象层   |
| `framework-data-jdbc`    | compile（传递）               | 默认实现 |
| `framework-data-mybatis` | compile（注释，按需启用）     | 备选实现 |
| `HikariCP` / `Druid`     | 传递（经 data-jdbc）          | 连接池   |
| `MyBatis Flex`           | 传递（经 data-mybatis，按需） | ORM      |

> **风险项（沿用 data-mybatis 文档标注）**：MyBatis Flex ≥1.11 对 Spring Boot 4 的兼容性 **未验证**。引入 mybatis
> 实现前需先做兼容性验证；若 Flex 未适配 Spring Boot 4.x，autoconfigure 提供 `FrameworkDataAutoConfiguration` 兜底装配（降级为
> data-jdbc
> 或原生 MyBatis）。

---

## 八、配置属性

本模块 **不定义配置属性**（零代码）。数据访问相关配置由传递的模块解析，本模块仅约定 **实现选择键**（由 boot-autoconfigure
读取）：

```yaml
framework:
  data:
    implementation: jdbc        # jdbc（默认）/ mybatis；两实现并存时的显式选择
    # 其余配置见 data-core / data-jdbc / data-mybatis 各自文档
```

---

## 九、使用方式

#### 9.1 最小引入（默认 JDBC 实现）

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-bom</artifactId>
            <version>4.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
<!-- Boot 运行时（starter 聚合 core/logger/autoconfigure） -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-boot-starter</artifactId>
</dependency>
<!-- 数据访问聚合入口：抽象 + JDBC 默认实现 -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-data</artifactId>
</dependency>
</dependencies>
```

```textmate

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Service
public class UserService {
    // 注入 data-core 抽象，实现由装配层按实现选择提供
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

#### 9.2 切换 MyBatis Flex 实现

```xml

<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data</artifactId>
    </dependency>
    <!-- 显式引入 mybatis 实现（启用聚合器中的注释块） -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-mybatis</artifactId>
    </dependency>
</dependencies>
```

```yaml
framework:
  data:
    implementation: mybatis    # 显式声明，避免与默认 JDBC 的歧义
```

#### 9.3 直接使用（跳过聚合器，等价路径）

```xml

<dependencies>
    <!-- 跳过 framework-data，直接引实现 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-jdbc</artifactId>
    </dependency>
</dependencies>
```

> 聚合器只是"入口便利"，不改变任何运行时行为；两类用法可自由混用于同一仓库的不同服务。

---

## 十、SPI 扩展点汇总

本模块 **无 SPI 扩展点**（零代码）。数据访问扩展点在 `framework-data-core` 定义（`RepositoryFactory` / `Dialect` /
`TransactionManager` 等），实现方式：

- 新数据源实现：新建模块实现 data-core SPI，并在聚合器中追加为备选实现；
- 新方言：实现 `Dialect` 接口注册（详见 data-core 文档 §十）。

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────────┐
│                    业务应用（Application）                      │
│  引入 framework-boot-starter + framework-data（可选聚合入口）   │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│  framework-data（本模块，纯 POM 聚合父模块）                    │
│  data-core（抽象） + data-jdbc（默认实现）/ data-mybatis（备选）│
└──────────────────────┬──────────────────────────────────────────┘
                       │ 运行时装配（条件注解择一）
┌──────────────────────▼──────────────────────────────────────────┐
│          framework-boot-autoconfigure                           │
│  FrameworkDataAutoConfiguration：按 framework.data.* 装配       │
└──────────────────────┬──────────────────────────────────────────┘
                       │ 版本
┌──────────────────────▼──────────────────────────────────────────┐
│   framework-bom（import scope）+ 根 pom（三方版本）             │
└─────────────────────────────────────────────────────────────────┘
```

**依赖方向**：业务 → starter（运行入口）+ framework-data（数据入口）→ autoconfigure → data-core 抽象 → jdbc/mybatis
实现。框架对外呈现"运行与数据两个聚合口"，内部模块组织对业务方透明。

---

## 修订记录

| 版本 | 说明 |
|:-----|:-----|
|      |      |
