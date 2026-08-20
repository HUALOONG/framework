# framework-bom 模块架构设计

> 文档元信息
> - **模块**：framework-bom
> - **关键词**：版本仲裁、dependencyManagement、import scope、版本治理
> - **描述**：框架对外版本仲裁中枢，只管理框架自身模块版本，第三方依赖版本统一由根 pom.xml 管理
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-bom` 是框架的 **版本仲裁（BOM）模块**，负责对外锁定框架全部内部模块的版本号，供下游项目通过
`<scope>import</scope>` 引入。 **它只管理框架自身模块，不混入任何第三方坐标**（第 36 轮最终决策）。

**核心价值**：

| 场景                   | 没有本模块                           | 有本模块                                             |
|:-----------------------|:-------------------------------------|:-----------------------------------------------------|
| 下游引入多个框架模块   | 每个依赖都要手写版本号，升级时逐个改 | 引入 BOM 后免版本号，`${framework.version}` 一处升级 |
| 框架内部模块版本一致性 | 各模块版本可能漂移                   | 版本单一来源，BOM 统一仲裁                           |
| 第三方依赖版本控制     | BOM 混入三方坐标 → 下游被锁版本      | 三方版本由根 pom 管理，下游自由选择                  |

**与核心模块的边界**：

| 模块              | 定位             | 特点                                     |
|:------------------|:-----------------|:-----------------------------------------|
| **framework-bom** | **版本仲裁**     | **纯 POM，只管框架模块版本**             |
| 根 pom.xml        | 三方依赖版本字典 | 管理 Spring Boot / HikariCP / Jackson 等 |
| framework-boot    | 超级聚合器       | 聚合全部子模块构建                       |

---

## 二、功能清单与依赖矩阵

| 功能             | 载体                    | 说明                                        |
|:-----------------|:------------------------|:--------------------------------------------|
| 框架模块版本锁定 | `dependencyManagement`  | 全部 `cn.jowen.framework:framework-*`       |
| 版本属性继承     | `<parent>`              | 继承根 pom 的 `${framework.version}` 等属性 |
| 下游免版本号     | `<scope>import</scope>` | 下游引入后直接使用框架模块                  |
| 三方版本隔离     | 不声明三方坐标          | 保证下游三方版本自由                        |

**依赖关系**：无任何运行时依赖；构建期依赖根 pom（parent）。

---

## 三、整体包结构

```text
framework-bom/
├─ pom.xml                    # 唯一文件（无 src 目录、无代码）
└─ README.md                  # 使用说明
```

---

## 四、各子包详细设计

纯 POM 模块无包结构，按设计层次说明：

#### 4.1 版本属性继承层

通过 `<parent>` 指向根 pom.xml，继承全部版本属性（`${framework.version}`、三方版本等）， **不重复声明 properties**，保证单一事实来源。

#### 4.2 框架模块版本管理层

`<dependencyManagement>` 只声明框架自身模块，使用 `${framework.version}` 统一版本：

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-core</artifactId>
            <version>${framework.version}</version>
        </dependency>
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-data-jdbc</artifactId>
            <version>${framework.version}</version>
        </dependency>
        <!-- ... 全部框架模块，禁止混入三方坐标 ... -->
    </dependencies>
</dependencyManagement>
```

#### 4.3 聚合与打包层

- 打包类型 `pom`，仅发布到私服供下游 import
- Maven 4 注意：同 Reactor 内 import 未安装 BOM 会被禁止，开发期先 `mvn install -pl framework-bom`

---

## 五、核心类关系图

```
┌────────────────────────────────────────────────────────────────┐
│                    根 pom.xml（parent）                        │
│  properties: framework.version / 三方版本号                    │
│  dependencyManagement: spring-boot-deps, HikariCP, Jackson...  │
└───────────────────────────┬────────────────────────────────────┘
                            │ <parent> 继承属性
┌───────────────────────────▼────────────────────────────────────┐
│                    framework-bom                               │
│  dependencyManagement（仅框架模块）                            │
│  framework-core:${framework.version}                           │
│  framework-data-*:${framework.version}                         │
│  framework-cache / i18n / plugin / extras ...                  │
└───────────────────────────┬────────────────────────────────────┘
                            │ <scope>import</scope>
┌───────────────────────────▼────────────────────────────────────┐
│                    下游项目（业务应用）                        │
│  获得框架模块版本锁定，三方版本自由声明                        │
└────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```
只管理，不依赖：
构建期：framework-bom → 根 pom.xml（parent 继承）
运行期：framework-bom 不参与任何运行时依赖
下游：业务应用 import framework-bom → 使用各框架模块
```

---

## 七、外部依赖

```xml
<!-- framework-bom/pom.xml -->
<project>
    <parent>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-boot</artifactId>
        <version>${framework.version}</version>
    </parent>

    <artifactId>framework-bom</artifactId>
    <packaging>pom</packaging>

    <dependencyManagement>
        <dependencies>
            <!-- 仅框架自身模块，版本统一 ${framework.version} -->
            <dependency>
                <groupId>cn.jowen.framework</groupId>
                <artifactId>framework-core</artifactId>
                <version>${framework.version}</version>
            </dependency>
            <!-- framework-logger / data-core / data-jdbc / data-mybatis / -->
            <!-- cache / i18n / plugin / extras / boot-autoconfigure / starter -->
        </dependencies>
    </dependencyManagement>
</project>
```

---

## 八、配置属性

| 属性                   | 来源   | 说明                         |
|:-----------------------|:-------|:-----------------------------|
| `${framework.version}` | 根 pom | 框架整体版本号，BOM 统一引用 |
| 三方版本号             | 根 pom | BOM 中不出现，保证下游自由   |

---

## 九、使用方式

```xml
<!-- 下游项目引入 BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.jowen.framework</groupId>
            <artifactId>framework-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
<!-- 引入后无需写版本号 -->
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-data-mybatis</artifactId>
</dependency>
</dependencies>
```

---

## 十、SPI 扩展点汇总

无 SPI。纯 POM 聚合器，不提供任何可扩展接口。

---

## 十一、与整体框架的关系

```
framework-boot（超级聚合器）
   ├─ framework-bom（对外版本仲裁）
   ├─ framework-core / logger / data-* / cache / i18n / plugin / extras
   └─ boot-autoconfigure / boot-starter
                ↓ import
       业务应用（下游项目）
```

**核心设计原则总结**：

1. **职责单一**：BOM 只管框架模块版本，三方依赖上移根 pom
2. **下游干净**：import 后三方版本自由，不被框架绑架
3. **单一事实来源**：所有版本号在根 pom properties 一处管理
