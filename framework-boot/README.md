# framework-boot 模块架构设计

> 文档元信息
> - **模块**：framework-boot
> - **关键词**：Boot 域聚合、POM 聚合、零代码、装配与入口、构建单元
> - **描述**：Boot 域聚合父模块，仅含 pom.xml 无代码，聚合 boot-autoconfigure / boot-starter 两个子模块
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-boot` 是 Boot 适配领域的 **聚合父模块（纯 POM，零代码）**，聚合两个 boot 子模块：

```text
framework-boot（聚合父模块，纯 POM）
├─ framework-boot-autoconfigure    # 自动装配逻辑（Boot 依赖唯一汇聚点）
└─ framework-boot-starter          # Starter 聚合入口（运行总开关）
```

**核心职责**：

1. **Boot 域统一构建单元**：一次 `mvn install -pl framework-boot -am` 构建整个 boot 家族（autoconfigure +
   starter）及其上游（core/logger 等），无需分别协调两个 boot 模块的构建；
2. **Boot 域依赖清单承载**：在 `<dependencies>` 中显式声明 boot 域的两个成员，形成"boot 域 = 装配层 +
   运行入口"的完整构成视图，供维护者与工具（依赖树分析、版本核对）使用；
3. **与 framework-data 对称**：框架对外呈现两个领域聚合口——`framework-data`（数据域）与 `framework-boot`（Boot
   域），组织结构一致、可读性一致。

**重要修正（相对早期设计的变更）**：

> `framework-boot` **不再承担"全仓库超级聚合"职责**。仓库级全量构建由 **根 pom（framework-parent）兼任 aggregator**（Maven
> 惯例：parent 与 aggregator 合一，在根 pom 的 `<modules>` 中列出全部 14 个模块），在仓库根目录执行一次 `mvn install`
> 即可构建全框架。本模块只管 boot 域。

**与相邻模块的边界**：

| 模块                           | 分工                                                       |
|:-------------------------------|:-----------------------------------------------------------|
| `framework-bom`                | 管版本（框架模块版本字典），不聚合实现                     |
| `framework-data`               | 管"数据访问域"聚合（data-core/jdbc/mybatis），与 Boot 无关 |
| **`framework-boot`（本模块）** | 管"Boot 适配域"聚合（autoconfigure/starter），纯 POM       |
| `framework-boot-starter`       | 管"启动"（Boot 运行时 + 装配层），是运行时入口本体         |
| 根 pom（framework-parent）     | 版本字典 + 全仓库 aggregator（modules 全量）               |

**核心价值**：

| 场景             | 没有本模块                                                      | 有本模块                          |
|:-----------------|:----------------------------------------------------------------|:----------------------------------|
| 构建 boot 域     | 逐个 `cd framework-boot-autoconfigure && mvn install` + starter | `-pl framework-boot -am` 一次构建 |
| 查看 boot 域构成 | 翻阅两个模块 pom                                                | 聚合器 dependencies 一目了然      |
| 域结构对称性     | data 有聚合、boot 无聚合，结构不对称                            | data/boot 双域聚合口对称          |

> **注意**：`framework-boot` 与 `framework-data` 一样，是 **构建/维护视角的聚合单元**；下游业务运行时 **不依赖**本模块（运行时入口是
> `framework-boot-starter`，版本入口是 `framework-bom`）。本模块不出现在业务依赖图中。

---

## 二、功能清单与依赖矩阵

| 功能         | 形式     | 依赖                                            | 说明                                                                         |
|:-------------|:---------|:------------------------------------------------|:-----------------------------------------------------------------------------|
| 自动装配逻辑 | 传递依赖 | framework-boot-autoconfigure                    | 11 个装配类 + EventBridge 双向桥接（Boot 依赖唯一汇聚点）                    |
| 运行聚合入口 | 传递依赖 | framework-boot-starter                          | core/logger/autoconfigure 运行聚合（starter 默认轻量，不含 data/cache/i18n） |
| Boot 运行时  | 传递依赖 | spring-boot-starter（经 autoconfigure/starter） | SB4 全栈适配（Jackson 3 / Micrometer 2.0 / 虚拟线程）                        |

**聚合策略**：

- **无选择规则**：boot 域两个子模块是包含关系（starter 依赖 autoconfigure），非互斥，全部传递；
- **不重复装配**：framework-boot 仅做依赖聚合，不触发任何 Bean 注册——装配逻辑全部在 `framework-boot-autoconfigure`，本模块不引入
  Spring；
- **与 data 域正交**：framework-boot 不依赖 framework-data，业务方按需分别引入两域。

---

## 三、模块结构

```text
framework-boot
├─ pom.xml                          # 唯一内容：聚合声明
└─ src/
   └─ (无 main/java / resources —— 零代码模块)
```

> 与 framework-bom / framework-data / framework-boot-starter 相同，聚合器一律零代码，避免重复初始化与类冲突。

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
        <artifactId>framework</artifactId>
        <version>0.1.0</version>
    </parent>

    <artifactId>framework-boot</artifactId>
    <packaging>pom</packaging>
    <name>framework-boot</name>
    <description>Framework Boot - Boot 域聚合父模块（零代码）</description>

    <dependencies>
        <!-- 装配层：Boot 依赖唯一汇聚点（11 个 AutoConfiguration + EventBridge） -->
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-boot-autoconfigure</artifactId>
        </dependency>

        <!-- 运行聚合入口：core / logger / autoconfigure 运行时聚合 -->
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

**设计取舍说明**：

- **采用 `<dependencies>` 而非 `<modules>`**：与 framework-data 完全一致——boot-autoconfigure / boot-starter
  是平级模块（不在本目录内），聚合器通过依赖声明表达"域构成"，而非目录包含；
- **starter 已含 autoconfigure，为何仍显式列出**：表达 boot 域"装配 + 入口"两个组成要素的完整视图（Maven 依赖去重自动处理，无重复
  jar）；同时保证 `-pl framework-boot -am` 能正确连带两个模块；
- **不写版本号**：全部由根 pom 的 dependencyManagement / framework-bom 裁决，与"版本字典上移"决策一致；
- **根 pom 兼任全仓库 aggregator**：根 pom 的 `<modules>` 按拓扑列出全部 14 个模块（framework-boot 与 framework-data
  均在其中），仓库根目录 `mvn install` 即全量构建。

---

## 五、核心类关系图

```text
┌───────────────────────────────────────────────────────────────┐
│             framework-boot（纯 POM 聚合父模块）               │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ framework-boot-autoconfigure（装配层）                 │   │
│  │  11 个 AutoConfiguration + EventBridge + Properties    │   │
│  │  ← Boot 依赖唯一汇聚点（SB4 / Jackson 3 / Micrometer） │   │
│  └──────────────────────────┬─────────────────────────────┘   │
│                             │ 依赖                            │
│  ┌──────────────────────────▼─────────────────────────────┐   │
│  │ framework-boot-starter（运行聚合入口，零代码）         │   │
│  │  core + logger + autoconfigure 运行时聚合              │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                               │
│  域对称：framework-data（数据域）⇄ framework-boot（Boot 域）  │
│  全仓库聚合：根 pom（framework-parent）modules 全量           │
└───────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L3（装配层）         framework-boot-autoconfigure
                      ▲ 依赖
L4（运行聚合入口）    framework-boot-starter
                      ▲ 聚合
L4（域聚合）         ★ framework-boot（本模块，纯 POM，构建/维护视角）
                      ▲ 全仓库聚合
                    根 pom（framework-parent，modules 全量 + 版本字典）
```

**模块间规则**：

1. **包含关系非互斥**：boot-autoconfigure 与 boot-starter 是依赖链（starter → autoconfigure），不涉及"二选一"（区别于 data 域
   jdbc/mybatis）；
2. **聚合器无运行时职责**：framework-boot 的依赖全部为 compile（传递），但自身不触发任何 Bean 注册——装配决策全部在
   boot-autoconfigure；
3. **与 data 域正交**：boot 域不依赖 data 域；业务方引入 `framework-boot`（若用构建视角）或 `framework-boot-starter`
   （运行视角）后按需追加 `framework-data`；
4. **不进入业务依赖图**：下游业务依赖 `framework-boot-starter`（运行）+ `framework-bom`（版本），不依赖 `framework-boot` 本模块。

---

## 七、外部依赖

| 依赖                                 | scope                            | 用途                      |
|:-------------------------------------|:---------------------------------|:--------------------------|
| `framework-boot-autoconfigure`       | compile（传递）                  | 装配层（Boot 依赖汇聚点） |
| `framework-boot-starter`             | compile（传递）                  | 运行聚合入口              |
| `spring-boot-starter`                | 传递（经 starter）               | SB4 运行时                |
| Jackson 3 / Micrometer 2.0 / Logback | 传递（经 starter/autoconfigure） | 序列化 / 可观测性 / 日志  |

> 本模块自身零外部依赖；版本全部由根 pom 与 framework-bom 裁决。

---

## 八、配置属性

本模块 **不定义配置属性**（零代码）。Boot 域相关配置由传递的模块解析（见 boot-autoconfigure 文档 §八，如
`framework.autoconfigure.*` / `framework.logger.*`）。

构建期用法：

```bash
# 构建整个 boot 域（连带上游 core/logger）
mvn install -pl framework-boot -am

# 全仓库构建（根 pom aggregator）
cd <repo-root>
mvn clean install
```

---

## 九、使用方式

#### 9.1 维护者：构建 boot 域

```bash
mvn install -pl framework-boot -am    # 构建 boot-autoconfigure + boot-starter 及其上游
```

#### 9.2 维护者：全仓库构建

```bash
cd <repo-root>                        # 根 pom（framework-parent）兼任 aggregator
mvn clean install                     # 拓扑序构建全部 14 个模块
```

#### 9.3 下游用户（重要）

```text
下游业务方【不依赖】framework-boot。引入方式固定为：
① framework-bom（import scope）管版本
② framework-boot-starter（+ 按需 data/cache/i18n/extras）管运行时
```

#### 9.4 仅使用装配层（无运行聚合）

```xml

<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-boot-autoconfigure</artifactId>
    </dependency>
</dependencies>
```

> 与 framework-data 相同，聚合器只是"构建/构成视图的便利"，不改变任何运行时行为。

---

## 十、SPI 扩展点汇总

本模块 **无 SPI 扩展点**（零代码）。Boot 域扩展点：

- 新增装配能力：在 `framework-boot-autoconfigure` 新增 `@AutoConfiguration` 类并登记到
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（详见 boot-autoconfigure 文档）；
- 新增启动入口：修改 `framework-boot-starter` 的依赖清单。

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│  根 pom（framework-parent）—— 版本字典 + 全仓库 aggregator  │
│  一次 mvn install → 14 个模块按拓扑构建                     │
└───────┬──────────────────────────────┬──────────────────────┘
        │ 聚合（构建期）               │ 版本
┌───────▼───────────────────┐  ┌───────▼──────────────────┐
│ framework-data（数据域）  │  │ framework-boot（Boot 域）│
│ core/jdbc/mybatis         │  │ autoconfigure/starter    │
└───────┬───────────────────┘  └───────┬──────────────────┘
        │ 运行时（业务按需）           │ 运行时
┌───────▼──────────────────────────────▼───────────────┐
│               业务应用（下游用户）                   │
│ framework-bom（版本） + framework-boot-starter       │
│ + framework-data / cache / i18n / extras（按需）     │
└──────────────────────────────────────────────────────┘
```

**一句话总结**：`framework-boot` 是 boot 域的 **聚合父模块**（与 `framework-data` 对称，纯 POM、构建/维护视角）；
`framework-bom` 是下游的"版本总开关"，`framework-boot-starter` 是应用的"运行总开关"，根 pom
是仓库的"构建总开关"——四者职责互不重叠。

---

## 修订记录

| 版本 | 说明 |
|:-----|:-----|
|      |      |
