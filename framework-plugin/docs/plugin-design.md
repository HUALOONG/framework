# framework-plugin 模块系统架构设计

**作者**：高见远（Gao · Architect）  
**日期**：2026-08-21  
**基线**：Spring Boot 4.1.0 + Java 21，包名 `cn.jowen.framework.plugin`

---

## Part A：系统架构

### 1. 实现方案

#### 1.1 核心挑战

本模块在 **零 Spring 强依赖**的前提下提供完整插件化能力，需同时解决三个维度的问题：

| 维度 | 挑战 | 解法 |
|------|------|------|
| 类加载隔离 | 插件类与宿主类互不可见，同时需按 `exported-packages` 委派父加载器 | 三层抽象：`PluginClassLoader`(抽象基类) → `FrameworkApiDelegateClassLoader`(按包委派) / `IsolatedClassLoader`(完全隔离) |
| 依赖解析 | 插件间存在 DAG 依赖，需拓扑排序 + 版本仲裁 + 循环检测 | `DependencyResolver` 独立实现，使用 Tarjan SCC 算法检测循环，Maven 风格 `VersionRange` 求交集 |
| 热部署 | 监听文件系统变更、防抖、策略切换 | `WatchService` + `DebounceTimer` + `HotSwapStrategy` 策略接口 |

#### 1.2 框架选型

- **JSON 解析**：复用 `framework-core` 已有的 Jackson 工具类（若 core 无则引入 `com.fasterxml.jackson.core:jackson-databind`，scope=test 或 optional）；`plugin.json` 为轻量 schema，采用手动 JSON 解析避免依赖，或使用 `java.util.json` (Java 21)。最终选择 **手动解析**（零新增依赖）。
- **SPI/扩展点**：复用 core 的 `@SPI` + `ExtensionLoader` 机制；plugin 模块的 `@ExtensionPoint`/`@Extension` 为 **语义同义包装注解**，内部委托 `ExtensionLoader`。
- **WatchService**：JDK 原生 `java.nio.file.WatchService`，零依赖。
- **Spring 桥接**：`PluginProperties`、`PluginApplicationContext` 标注 `@ConditionalOnClass(ApplicationContext.class)`，`spring-context` 依赖声明为 `<optional>true</optional>`。

#### 1.3 架构分层

```
┌─────────────────────────────────────────────┐
│           framework-boot-autoconfigure       │  P2
│  (Spring 自动装配 + Actuator 端点)            │
├──────────────┬──────────────┬───────────────┤
│  config/     │  context/    │  hotswap/     │
│  (Spring ON) │ (Spring ON)  │  WatchService │
├──────────────┴──────────────┴───────────────┤
│  extension/  │  dependency/ │  event/       │
│  (@Extension)│ (DAG 解析)   │ (EventBus集成)│
├──────────────┴──────────────┴───────────────┤
│  classloader/         │  descriptor/        │
│  (类加载隔离)          │  (plugin.json 解析) │
├───────────────────────┴─────────────────────┤
│  PluginManager │ PluginLoader │ Plugin      │
│  (现有骨架类增强)                         │
└─────────────────────────────────────────────┘
         全部依赖 framework-core（零 Spring API）
```

