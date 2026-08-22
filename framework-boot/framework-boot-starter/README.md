# framework-boot-starter 模块架构设计

> 文档元信息
> - **模块**：framework-boot-starter
> - **关键词**：Starter、POM 聚合、零代码、依赖入口、版本管理
> - **描述**：框架 Starter 聚合入口，仅含 pom.xml 无代码，用户引入一个依赖获得框架全部默认能力
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-boot-starter` 是框架的 **Starter 聚合入口模块（L5，最高层）**， **仅包含 `pom.xml`，不含任何 Java 代码与资源文件**
。它的职责是：

1. 聚合框架默认能力（core/logger/autoconfigure）为单一依赖，用户一行引入即可启动；
2. 承接 Spring Boot 官方 Starter 依赖（`spring-boot-starter`），形成完整的 Boot 运行时；
3. 与 `framework-bom` 配合完成版本管理：starter 只管依赖声明，版本号统一由 BOM/根 pom 管控。

**核心价值**：

| 场景     | 没有本模块                    | 有本模块                          |
|:---------|:------------------------------|:----------------------------------|
| 引入框架 | 手动引 4+ 个依赖并配版本      | 一个依赖 `framework-boot-starter` |
| 依赖遗漏 | 漏引 autoconfigure 导致不装配 | POM 聚合保证装配完整              |
| 版本混乱 | 各模块版本人工对齐            | BOM 统一管理                      |

**设计原则**：

- **零代码**：不提供任何类、注解、资源，避免重复初始化；
- **最小聚合**：默认只聚合 core + logger + autoconfigure（+ Boot 官方 starter），数据访问、缓存、i18n 等 **按需**
  由业务方补充引入，保持默认依赖轻量；
- **版本后置**：starter 内不写版本号，全部由 `framework-bom`（import scope）与根 pom 的 dependencyManagement 决定。

---

## 二、功能清单与依赖矩阵

| 功能        | 形式     | 依赖                         | 说明                       |
|:------------|:---------|:-----------------------------|:---------------------------|
| Boot 运行时 | 传递依赖 | spring-boot-starter          | 官方基础 starter           |
| 框架地基    | 传递依赖 | framework-core               | SPI/异常/断言              |
| 日志能力    | 传递依赖 | framework-logger             | facade + 脱敏 + MDC        |
| 自动装配    | 传递依赖 | framework-boot-autoconfigure | 全部 @Conditional 装配入口 |
| 数据访问    | 按需引入 | framework-data-*             | 不默认聚合（二选一）       |
| 缓存        | 按需引入 | framework-cache              | 不默认聚合                 |
| 国际化      | 按需引入 | framework-i18n               | 不默认聚合                 |
| 插件化      | 按需引入 | framework-plugin             | 不默认聚合（默认关闭）     |
| 工具集      | 按需引入 | framework-extras             | 不默认聚合                 |

---

## 三、模块结构

```text
framework-boot-starter
├─ pom.xml                          # 唯一内容：依赖聚合声明
└─ src/
   └─ (无 main/java / resources —— 零代码模块)
```

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
        <artifactId>framework-boot</artifactId>     <!-- 父 POM：统一 properties + dependencyManagement（版本由根 pom ${revision} 控制） -->
        <version>${revision}</version>
    </parent>

    <artifactId>framework-boot-starter</artifactId>
    <packaging>jar</packaging>
    <name>framework-boot-starter</name>
    <description>Framework Boot Starter - 聚合入口（零代码）</description>

    <dependencies>
        <!-- Boot 官方基础 starter：提供 SpringApplication / 配置绑定 / 日志桥接 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- 框架地基 -->
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-core</artifactId>
        </dependency>

        <!-- 自动装配层（Boot 依赖汇聚点） -->
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-boot-autoconfigure</artifactId>
        </dependency>

        <!-- 日志能力 -->
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-logger</artifactId>
        </dependency>
    </dependencies>
</project>
```

> **说明**：所有依赖均省略 `<version>`——由 `framework-bom`（import scope）+ 根 pom dependencyManagement 统一裁决，与"BOM
> 只管理框架模块版本、三方版本上移根 pom"的决策一致。

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────────────────────────┐
│              framework-boot-starter（纯 POM）                │
│                                                             │
│  依赖聚合（传递）：                                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  spring-boot-starter（官方）                          │   │
│  │  └─ spring-boot / spring-boot-autoconfigure          │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  framework-boot-autoconfigure（装配层）               │   │
│  │  └─ BootAutoConfiguration + 8 个装配类           │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  framework-core（地基）→ framework-logger（日志）      │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
│  业务方按需补充（不传递）：                                   │
│  framework-data-jdbc / framework-data-mybatis / cache       │
│  i18n / plugin / extras                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0~L3（实现层）     framework-core → logger/data-*/cache/i18n → plugin/extras
                      ▲
L4（装配层）         framework-boot-autoconfigure（唯一 Boot 依赖点）
                      ▲
L5（聚合层）         ★ framework-boot-starter（本模块，纯 POM）
                      ▲
L6（版本层）         framework-bom（import scope）+ 根 pom（三方版本）
```

**模块间规则**：

- starter 依赖 autoconfigure，autoconfigure 编译期依赖各实现模块（optional，不向业务方传递），运行时由 @ConditionalOnClass 按需装配；
- 数据/缓存/i18n/plugin/extras 不在 starter 默认聚合内，避免无 Redis/无 DB 场景下引入无用传递依赖；
- starter 不参与版本管理（无 dependencyManagement 职责），版本问题一律上抛 BOM/根 pom。

---

## 七、外部依赖

| 依赖                           | scope           | 用途            |
|:-------------------------------|:----------------|:----------------|
| `spring-boot-starter`          | compile（传递） | Boot 运行时基础 |
| `framework-core`               | compile（传递） | 地基            |
| `framework-boot-autoconfigure` | compile（传递） | 装配层          |
| `framework-logger`             | compile（传递） | 日志能力        |

> 无测试依赖（零代码模块）；`spring-boot-maven-plugin` 不配置（无需可执行 jar，若业务方需要可自行添加 repackage）。

---

## 八、配置属性

本模块 **不定义任何配置属性**（零代码）。生效的配置全部来自传递的 autoconfigure 层：

```yaml
framework:
  enabled: true                # 由 framework-boot-autoconfigure 解析
  logger: { ... }
  data: { ... }
  cache: { ... }
  i18n: { ... }
```

---

## 九、使用方式

#### 9.1 最小启动（仅默认能力）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-bom</artifactId>
            <version>0.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-boot-starter</artifactId>
    </dependency>
</dependencies>
```

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

> **可选增强**：健康检查（`/actuator/health` 含框架指标）与插件端点（`/actuator/frameworkPlugins`）需业务方另行引入
> `spring-boot-starter-actuator` 后由 autoconfigure 层自动装配。

#### 9.2 完整能力（+ 数据访问 + 缓存 + i18n）

```xml
<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-boot-starter</artifactId>
    </dependency>
    <!-- 按需补充实现 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-data-mybatis</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-cache</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-i18n</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-extras</artifactId>
    </dependency>
</dependencies>
```

#### 9.3 领域 Starter（可选扩展）

```text
若需要按业务域聚合（如 framework-boot-starter-data、framework-boot-starter-web），
参考本模块模式新建 POM 聚合模块即可，保持"零代码 + 版本后置"约定。
```

---

## 十、SPI 扩展点汇总

本模块 **无 SPI 扩展点**（零代码）。其扩展方式为：

- 新增聚合依赖（仿照本模块新建领域 starter）；
- 定制装配（通过 autoconfigure 层的 `*Customizer` 与 `@ConditionalOnMissingBean`）。

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│                    业务应用（Application）                    │
│  引入 framework-boot-starter（+ 按需实现模块）               │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│   ★ framework-boot-starter（本模块，纯 POM 聚合）            │
│   用户唯一接触点 · 零代码 · 版本后置                         │
└──────────────────────────┬──────────────────────────────────┘
                           │ 传递依赖
┌──────────────────────────▼──────────────────────────────────┐
│              framework-boot-autoconfigure                    │
│  @AutoConfiguration 按需装配 → 各实现模块 → framework-core   │
└──────────────────────────┬──────────────────────────────────┘
                           │ 版本管理
┌──────────────────────────▼──────────────────────────────────┐
│   framework-bom（import scope）+ 根 pom（三方版本）          │
└─────────────────────────────────────────────────────────────┘
```

**依赖方向**：业务 → starter（POM）→ autoconfigure → 实现模块 → core。starter 是框架对外的"一扇门"，内部模块如何组织对业务方完全透明；新增模块只需在
starter 或领域 starter 中追加依赖即可扩展。
