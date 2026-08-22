# framework-plugin 模块 PRD

**产品名称**：Jowen Framework — plugin 插件化扩展模块
**模块路径**：`framework-plugin/src/main/java/cn/jowen/framework/plugin/`
**基线**：Spring Boot 4.1.0 + Java 21，包名 `cn.jowen.framework.plugin`
**文档作者**：许清楚（Xu · Product Manager）
**日期**：2026-08-21

---

## 一、产品目标与定位

`framework-plugin` 是框架的 **L3 插件化扩展模块**，为上层框架和业务方提供一套**零 Spring 强依赖**的插件生命周期管理体系。核心解决三个问题：插件 jar 动态加载与类加载隔离、插件间依赖解析与版本仲裁、插件生命周期事件的可观测性。Spring 子容器、Actuator 端点等能力由 `framework-boot-autoconfigure` 按需桥接，plugin 模块本身保持对非 Spring 宿主的可用性。

---

## 二、用户故事

| ID | 角色 | 用户故事 | 验收标准 |
|----|------|---------|---------|
| US-01 | 框架使用者 | 将插件 jar 放入指定目录后，框架自动发现并启动插件 | `plugins-dir` 指向的目录中新建 jar 文件，`PluginManager.all()` 在 `debounce-interval` 内返回该插件实例，状态为 `STARTED` |
| US-02 | 框架使用者 | 插件卸载时其 ClassLoader 及其加载的所有类可被 GC 回收 | `unregister(pluginId)` 后，引用该插件 ClassLoader 的对象置 null，执行 GC 后该 ClassLoader 不在堆中（通过 `ClassLoader.getSystemClassLoader().getClass().getName()` 等间接证据验证） |
| US-03 | 框架使用者 | 插件声明依赖了另一个插件，且该依赖未满足时，当前插件不被加载 | `dependency` 列表中某插件 id 不存在于已注册表时，加载抛出明确错误（含缺失插件 id 信息），不进入 `STARTING` 状态 |
| US-04 | 框架使用者 | 热部署：替换 plugins 目录下某插件 jar，系统自动停止旧版本并启动新版本 | 修改 jar 文件（或替换为同 id 新版本），3s 内旧插件执行 `destroy()`，新插件执行 `afterPropertiesSet()`，版本号更新 |
| US-05 | 框架使用者 | 多个插件通过 `@Extension` 实现同一个 `@SPI` 扩展点，宿主侧按 order 获取所有实现 | 注册两个同扩展点插件后，`getExtension(ExtensionPoint.class)` 返回两个实例，按 `order` 升序排列；`@NullMarked` 约束下无 NPE |
| US-06 | 框架使用者 | 插件通过 plugin.json 描述符声明 `exported-packages`，框架加载时按委派策略处理 | descriptor 解析后 `exportedPackages` 非空，`FrameworkApiDelegateClassLoader.loadClass()` 对该包路径内的类委托父加载器，对插件自身类由自身加载 |
| US-07 | 框架运维者 | 通过 Actuator 端点查看当前所有插件的健康状态与列表 | `GET /actuator/plugins` 返回 JSON，包含每个插件的 `id`、`version`、`state`、`healthy` 字段；`PluginHealthIndicator` 在 Actuator 健康端点中体现各插件状态 |
| US-08 | 框架运维者 | 通过 Actuator 端点启动/停止/重启单个插件，无需重启应用 | `POST /actuator/plugins/{id}/start`、`/stop`、`/restart` 均调用 `PluginManager` 对应方法，响应中包含操作后的插件状态 |

---

## 三、需求池

### P0 — Must Have（当前阶段必须交付）

| 编号 | 需求描述 | 关联用户故事 |
|------|---------|------------|
| P0-1 | **现有骨架类保留并补全**：`Plugin`、`PluginDescriptor`、`PluginLoader`、`PluginManager`、`DefaultPluginManager` 保留现有接口签名，在此基础上扩展（见重构清单） | US-01/02/03 |
| P0-2 | **依赖解析**：实现 `DependencyResolver`（DAG 拓扑排序 + 循环检测 + 未满足依赖拒绝加载并给出清晰错误信息）；实现 `VersionRange`（Maven 风格 `[1.0.0,2.0.0)`） | US-03 |
| P0-3 | **插件事件总线集成**：基于 core 的 `EventBus`，发布 `PluginLoadedEvent`、`PluginStartedEvent`、`PluginStoppedEvent`、`PluginUnloadedEvent`、`PluginInstallEvent` 五个事件；提供 `PluginEventPublisher` 封装 | US-07 |
| P0-4 | **扩展机制基础**：实现 `@ExtensionPoint`（注解，标记扩展点接口）、`@Extension`（注解，标记实现类）、`ExtensionRegistry`（运行时注册表）；支持按 extensionPointId 查询实现列表，按 order 排序 | US-05 |
| P0-5 | **PluginDescriptor 增强**：在现有 record 基础上增加 `exportedPackages`（List\<String\>）和 `springEnabled`（boolean）字段，保持向后兼容（默认空列表、false） | US-06 |
| P0-6 | **配置属性**：`PluginProperties`（`@ConfigurationProperties(prefix = "framework.plugin")`），包含 `enabled`、`pluginsDir`、`hotSwap`（sub-config）、`classLoading`（sub-config）、`spring`（sub-config）字段；对应 `PluginClassLoadingProperties` 内部类 | — |
| P0-7 | **现有测试兼容**：`PluginManagerTest` 三个测试方法保持通过，新增测试覆盖 P0 级新功能 | — |

### P1 — Should Have（建议在本迭代交付）

| 编号 | 需求描述 | 关联用户故事 |
|------|---------|------------|
| P1-1 | **类加载隔离策略**：实现 `PluginClassLoader`（抽象基类，封装 URLClassLoader）、`FrameworkApiDelegateClassLoader`（按 exported-packages 委派父加载器）、`IsolatedClassLoader`（完全隔离，child-first）；`PluginLoader` 使用 `PluginClassLoader` 而非直接使用 `URLClassLoader` | US-02/06 |
| P1-2 | **SharedLibraryResolver**：按 Maven GAV 从本地 Maven 仓库缓存解析共享库 jar 路径，供 `PluginClassLoader` 合并到 URL 列表 | US-06 |
| P1-3 | **plugin.json 描述符解析**：`PluginJsonDescriptorParser` 从 JAR 内 `/META-INF/plugin/plugin.json` 解析为 `PluginDescriptor`；schema 与 pom 中一致 | US-06 |
| P1-4 | **热部署基础能力**：`PluginWatchService` 基于 JDK `WatchService` 监听 `plugins-dir`，支持 `RESTART` 策略；防抖 3s 由 `DebounceTimer` 实现 | US-01/04 |
| P1-5 | **Spring 子容器（可选）**：`PluginApplicationContext` 在 `spring.enabled=true` 时创建子容器，parent = 宿主 `ApplicationContext`；插件可访问宿主 Bean，卸载时 close | US-07 |
| P1-6 | **AbstractPlugin**：提供抽象基类，内含 `descriptor`、`context`、`state` 字段，模板方法 `init/start/stop`，减少插件实现样板代码 | US-01 |

### P2 — Nice to Have（后续迭代）

| 编号 | 需求描述 | 关联用户故事 |
|------|---------|------------|
| P2-1 | **Actuator 集成**：`PluginHealthIndicator` + `/actuator/plugins` 端点（start/stop/restart/install/uninstall）；位于 `framework-boot-autoconfigure`，plugin 模块只提供数据源接口 | US-07/08 |
| P2-2 | **plugin.yaml 支持**：通过 SnakeYAML optional 依赖支持 YAML 格式描述符，与 JSON 等价 | — |
| P2-3 | **`PluginState` 枚举**：`CREATED/STARTING/STARTED/STOPPING/STOPPED/FAILED/DISABLED`；状态机转换规则 | — |
| P2-4 | **`PluginContext` 接口**：`getPluginId()`、`getDescriptor()`、`getSharedData()`、`publishEvent()` 等上下文方法 | — |
| P2-5 | **`PluginPackage` 打包规范工具**：`extractPluginJar`、`calculateChecksum`、`isValidPluginId` 等辅助方法 | — |

---

## 四、非功能需求

| 维度 | 要求 |
|------|------|
| **零 Spring 核心依赖** | plugin 模块核心逻辑（api/descriptor/loader/resolver/lifecycle/event/hotswap）不引入任何 Spring API；`spring-context` 依赖必须为 `<optional>true</optional>` |
| **空安全** | 全模块 `@NullMarked`，API 层参数 `@NonNull`，返回类型遵循 JSpecify 约定 |
| **测试覆盖率** | P0 功能单测覆盖率 ≥ 80%，P1 功能 ≥ 60%；依赖解析器需覆盖循环检测边界用例 |
| **向后兼容** | `PluginDescriptor` 新增字段须有默认值；`PluginManager` 接口方法签名不变 |
| **构建约束** | 构建命令：`JAVA_HOME="D:\95_Programs\99_Runtimes\Java\liberica-21.0.11" "D:\95_Programs\99_Runtimes\Maven\v3.9.16\bin\mvn.cmd" -pl framework-plugin test`，所有测试通过方可合入 |
| **类加载隔离验证** | `FrameworkApiDelegateClassLoader` 须通过单元测试验证：核心包委派给父、隐藏包抛 `ClassNotFoundException`、插件自身类正常加载 |

---

## 五、待确认问题

| # | 问题 | 影响范围 |
|---|------|---------|
| Q-1 | `PluginDescriptor` 当前为 `record`，新增 `exportedPackages` 和 `springEnabled` 字段后，是否需要同步扩展测试中使用的静态工厂方法 `of()`？ | P0-5 |
| Q-2 | `DefaultPluginManager` 重构时，依赖解析（P0-2）应在 `register()` 前还是 `register()` 内部执行？前者更灵活（允许批量加载），后者更简单 | P0-2 |
| Q-3 | `SharedLibraryResolver` 的缓存目录：是否使用 `{user.home}/.m2/repository` 路径，还是允许通过配置 `shared-libraries-cache-dir` 自定义？ | P1-2 |
| Q-4 | 热部署的 `debounce-interval` 默认值 3s 是否足够？对于大 jar 文件下载场景是否需要支持可配置上限？ | P1-4 |
| Q-5 | `@ExtensionPoint` 与 core 的 `@SPI` 关系：是否直接在 plugin 模块内复用 `@SPI` 作为扩展点声明（README 4.7 节方案），还是新增 `@ExtensionPoint` 注解做包装？ | P0-4 |
