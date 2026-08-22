# framework-plugin 模块架构设计

> 文档元信息
> - **模块**：framework-plugin
> - **关键词**：插件化、类加载隔离、依赖解析、生命周期、扩展点、热部署
> - **描述**：插件发现、加载、隔离、生命周期管理、依赖解析与插件间通信，支持动态扩展框架能力
> - **基线**：Spring Boot 4.x + Java 21（插件系统自身不依赖 Spring，通过自动装配层桥接）
> - **架构决策**：插件系统核心不依赖 Spring；Spring 子容器作为可选能力（optional spring-context）

---

## 一、模块定位

`framework-plugin` 是框架的 **插件化扩展模块（L3）**
，提供插件发现、加载、隔离、生命周期管理、依赖解析与插件间通信能力。让框架核心能力被业务方或第三方以"插件"形式动态扩展，无需修改框架源码。

**核心价值**：

| 场景                 | 没有本模块                             | 有本模块                            |
|:---------------------|:---------------------------------------|:------------------------------------|
| 业务方想扩展框架能力 | fork 框架源码或写一堆 `@Configuration` | 开发插件 jar，放入 plugins 目录即可 |
| 多租户不同功能       | if-else 硬编码                         | 按租户加载不同插件组合              |
| 功能按需部署         | 全量打包                               | 只部署需要的插件                    |
| 第三方集成           | 侵入框架代码                           | 以插件形式无侵入接入                |

**关键架构决策（依赖 Spring 的边界）**：

| 方案                     | 说明                                                                               | 选择                                        |
|:-------------------------|:-----------------------------------------------------------------------------------|:--------------------------------------------|
| **A. 插件系统零 Spring** | 插件加载/隔离/生命周期纯 ClassLoader + SPI 实现；Spring 仅作为"宿主容器"被插件访问 | ✅ 核心设计（默认）                         |
| B. 插件 Spring 子容器    | 每个插件创建独立 Spring 子容器（parent = 宿主容器）                                | 可选能力，`spring-context` 以 optional 引入 |

> `framework-plugin` 本体 **不依赖** Spring Framework；只有启用 Spring 子容器（
> `framework.plugin.spring.enabled=true`）时才按需加载 `spring-context`，且该依赖由 `framework-boot-autoconfigure`
> 统一提供，插件模块保持中立。

**与核心模块的边界**：

| 模块                         | 定位           | 特点                             |
|:-----------------------------|:---------------|:---------------------------------|
| **framework-plugin**         | **插件化扩展** | **动态加载、隔离、热部署**       |
| framework-core               | 基础设施       | 本模块依赖其 SPI/Event/Exception |
| framework-boot-autoconfigure | 装配层         | 承载插件系统的 Spring 桥接       |

---

## 二、功能清单与依赖矩阵

| 功能         | 子包       | 核心依赖             | 可选依赖                  |
|:-------------|:-----------|:---------------------|:--------------------------|
| 插件核心抽象 | api        | framework-core       | —                         |
| 描述与元数据 | descriptor | —                    | SnakeYAML（plugin.yaml）  |
| 类加载隔离   | loader     | framework-core       | —                         |
| 依赖解析     | resolver   | —                    | —                         |
| 生命周期管理 | lifecycle  | —                    | —                         |
| 注册中心     | registry   | framework-core SPI   | —                         |
| 扩展点机制   | extension  | —                    | —                         |
| 插件上下文   | context    | —                    | spring-context（可选）    |
| 插件事件     | event      | framework-core Event | —                         |
| 热部署       | hotswap    | —                    | —                         |
| 配置与装配   | config     | Spring Boot 4        | Actuator（端点/健康检查） |
| 工具         | support    | framework-core       | —                         |

---

## 三、整体包结构

```text
framework-plugin
└─ src/main/java/com/framework/plugin/
   ├─ api/            # Plugin / PluginContext / PluginManager / PluginState
   ├─ descriptor/     # PluginDescriptor / PluginVersion / PluginDependency / VersionRange / ExtensionPointDescriptor / ExtensionDescriptor / PluginConfigurationDescriptor / PluginDescriptorLoader
   ├─ loader/         # PluginClassLoader / SharedClassLoader / PluginLoader / PluginInfo / ClassLoadingStrategy
   ├─ resolver/       # DependencyResolver / ResolutionResult / DependencyConflict / VersionArbitrator / DependencyGraph
   ├─ lifecycle/      # PluginLifecycleManager / PluginStateTransition / PluginHealthChecker
   ├─ registry/       # PluginRegistry / ExtensionRegistry / ExtensionPoint / Extension
   ├─ extension/      # 扩展桥接：@Extension / @ExtensionScan / ExtensionScanner / ExtensionDefinition / ExtensionFactory（扩展点声明复用 core @SPI）
   ├─ context/        # DefaultPluginContext / PluginConfiguration / SharedData / PluginSpringContextFactory / PluginBeanPostProcessor
   ├─ event/          # PluginEvent / PluginLoadedEvent / PluginStartedEvent / PluginFailedEvent / ... / PluginEventListener
   ├─ hotswap/        # PluginHotSwapManager / HotSwapStrategy / PluginFileWatcher / HotSwapContext
   ├─ config/         # PluginProperties（自动装配在 boot-autoconfigure）
   └─ support/        # PluginUtils / PluginPackage / ValidationError / AbstractPlugin
```

---

## 四、各子包详细设计

#### 4.1 api/ — 插件核心抽象

##### 定位

插件系统的门面契约。

```text
cn.jowen.framework.plugin.api
├─ Plugin                  #插件核心接口
│   ├─ start(PluginContext context) / stop()
│   ├─ getDescriptor() ->PluginDescriptor
│   ├─ getState() ->PluginState
│   ├─ getExtensions() ->List<Extension>
│   └─ getExtensionPoints() ->List<ExtensionPoint>
├─ PluginContext           #插件上下文
│   ├─ getPluginId() / getPluginDescriptor() / getConfiguration()
│   ├─ getSharedData() ->SharedData            #插件间共享数据
│   ├─ getPluginManager() ->PluginManager
│   ├─ getApplicationClassLoader() / getPluginClassLoader()
│   ├─ getSpringContext() ->ApplicationContext  #仅启用子容器时非 null
│   └─ publishEvent(PluginEvent event)
├─ PluginManager           #插件管理器（核心门面）
│   ├─ loadPlugins(Path pluginsDir) / loadPlugin(Path)
│   ├─ unloadPlugin /startPlugin /stopPlugin /restartPlugin
│   ├─ getPlugin /getPlugins /getPluginsByState
│   ├─ getExtension(String extensionPointId) / <T> getExtensions(Class<T>)
│   └─ registerExtensionPoint(ExtensionPoint point)
└─ PluginState             #状态枚举
    ├─ CREATED /STARTING /STARTED
    ├─ STOPPING /STOPPED /FAILED /DISABLED
```

#### 4.2 descriptor/ — 插件描述与元数据

##### 定位

插件声明信息的模型与加载。

```textmate
cn.jowen.framework.plugin.descriptor
├─ PluginDescriptor                #插件描述符
│   ├─ pluginId /pluginName /version /description
│   ├─ author /license /pluginClass
│   ├─ requires /optionalRequires（PluginDependency 列表）
│   ├─ provides /extensionPoints /extensions
│   ├─ configuration（PluginConfigurationDescriptor）
│   └─ enabledByDefault
├─ PluginVersion                   #语义化版本：major.minor.patch[-preRelease]+compareTo /isCompatible /parse
├─ PluginDependency                #插件依赖：pluginId +versionRange +optional
├─ VersionRange                    #版本范围："[1.0,2.0)"+contains /parse
├─ ExtensionPointDescriptor        #扩展点描述：id /interfaceName /singleton
├─ ExtensionDescriptor             #扩展描述：id /extensionPointId /className /order /properties
├─ PluginConfigurationDescriptor   #配置项描述：key /type /defaultValue /required /description
└─ PluginDescriptorLoader          #加载器：plugin.json（默认）/plugin.yaml /MANIFEST.MF +

validate()
```

**plugin.json 示例**：

```json
{
  "pluginId": "com.example.payment-alipay",
  "pluginName": "支付宝支付插件",
  "version": "1.2.0",
  "pluginClass": "com.example.payment.alipay.AlipayPlugin",
  "requires": [
    {
      "pluginId": "com.example.payment-core",
      "versionRange": "[1.0,2.0)"
    }
  ],
  "provides": [
    "payment.alipay"
  ],
  "extensions": [
    {
      "id": "alipay-payment-provider",
      "extensionPointId": "payment.provider",
      "className": "com.example.payment.alipay.AlipayPaymentProvider",
      "order": 10,
      "properties": {
        "channel": "alipay"
      }
    }
  ],
  "configuration": {
    "items": [
      {
        "key": "appId",
        "type": "string",
        "required": true,
        "description": "支付宝 AppID"
      },
      {
        "key": "sandbox",
        "type": "boolean",
        "defaultValue": "false"
      }
    ]
  }
}
```

#### 4.3 loader/ — 插件加载器（类加载隔离）

##### 定位

类加载隔离是插件系统的核心机制。

```textmate
cn.jowen.framework.plugin.loader
├─ PluginClassLoader         #插件类加载器（继承 URLClassLoader，可配置加载策略）
│   ├─ parent /sharedClassLoader /pluginUrls
│   ├─ exportedPackages（对外暴露）/importedPackages（从宿主导入）/hiddenClasses（禁止加载）
│   └─ loadClass 策略（见下方优先级）
├─ SharedClassLoader         #共享类加载器：插件间共享第三方库（Jackson/Guava），避免版本冲突
├─ PluginLoader              #加载器：解析描述符 →校验 → 创建 ClassLoader →实例化主类
├─ PluginInfo                #加载信息：descriptor +classLoader +instance +pluginPath +loadedAt
└─ ClassLoadingStrategy      #策略：PARENT_FIRST /CHILD_FIRST /FRAMEWORK_API_DELEGATE（默认推荐）
```

**类加载优先级（FRAMEWORK_API_DELEGATE）**：

1. 已加载的类 → 直接返回；
2. `cn.jowen.framework.core.**` / `cn.jowen.framework.plugin.api.**` → 委派宿主（保证框架接口一致）；
3. 共享库包 → 委派 SharedClassLoader；
4. 隐藏类（如 `javax.servlet.**`）→ 抛 `ClassNotFoundException`；
5. 插件自身 jar / lib → 自身加载（打破双亲委派）；
6. 兜底 → 委派宿主。

#### 4.4 resolver/ — 依赖解析与版本仲裁

##### 定位

插件依赖图构建、拓扑排序、循环检测与版本仲裁。

```textmate
cn.jowen.framework.plugin.resolver
├─ DependencyResolver            #解析器：构建 DAG →拓扑排序 →版本仲裁 →返回加载顺序 +冲突报告
├─ ResolutionResult              #结果：loadOrder /conflicts /missing /isResolvable()
├─ DependencyConflict            #冲突：pluginId /requiredBy /requiredVersions /resolved
├─ VersionArbitrator             #仲裁：计算版本范围交集 →返回最高兼容版本
└─ DependencyGraph               #图：addNode /addEdge /topologicalSort /detectCycles /getTransitiveDependencies
```

#### 4.5 lifecycle/ — 插件生命周期管理

##### 定位

状态机驱动的生命周期。

```textmate
cn.jowen.framework.plugin.lifecycle
├─ PluginLifecycleManager      #生命周期管理器
│   ├─ initialize：注入 context →CREATED
│   ├─ start：校验依赖已启动 → 创建 Spring 子容器（可选）→注册扩展点 → plugin.start() →STARTED →发布事件
│   ├─ stop：注销扩展点 → plugin.stop() →销毁子容器 →STOPPED
│   ├─ restart：stop +start
│   └─ destroy：stop +关闭 ClassLoader +移除注册
├─ PluginStateTransition       #状态转换规则：CREATED→STARTING→STARTED /STARTED→STOPPING→STOPPED /ANY→FAILED /FAILED→STARTING
└─ PluginHealthChecker         #健康检查：状态 +依赖满足 +自定义 HealthCheckable
```

**状态机**：

```text
CREATED ──start──▶ STARTING ──▶ STARTED
                          │         │
                          ▼         ▼ stop
                       FAILED ◀── STOPPING ──▶ STOPPED
                          │                      │
                          └────retry─────────────┘
```

#### 4.6 registry/ — 插件注册中心

##### 定位

插件与扩展的统一注册与查询。

```textmate
cn.jowen.framework.plugin.registry
├─ PluginRegistry        #插件注册：唯一性校验 /register /unregister /getPlugin /getPluginsByState
├─ ExtensionRegistry     #扩展注册：扩展点唯一性 /扩展类型校验 / 按 order 排序 / getExtensions(Class<T>)
├─ ExtensionPoint        #扩展点：id +type +description +singleton + of(id, type)
└─ Extension             #扩展：id +extensionPointId +instance +order +pluginId +properties
```

#### 4.7 extension/ — 扩展机制（复用 core @SPI，跨 ClassLoader 桥接）

##### 定位

**不另立扩展点标注体系**（冲突修正决议 #5）。扩展点声明统一使用 `core.spi.@SPI`；本子包只负责 **跨 ClassLoader
的桥接与实例化**。

```textmate
cn.jowen.framework.plugin.extension
├─ @Extension            #扩展实现标注：id / extensionPoint(=core @SPI 的 name) /order /properties
├─ @ExtensionScan        #扫描包路径：basePackages
├─ ExtensionScanner      #扫描器：按注解扫描 / 按 plugin.json 的 extensions 配置加载
├─ ExtensionDefinition   #定义：id /extensionPointId /className /order /properties +  instantiate(ClassLoader)
└─ ExtensionFactory      #实例化工厂：ClassLoader 加载 → 构造器注入 PluginContext →配置注入 → 子容器 Bean 获取
```

**桥接约定**：

- 宿主侧扩展点 = `@SPI("xxx")` 接口（core 语义，全局唯一）；
- 插件侧 `@Extension(extensionPoint = "xxx")` 绑定该 SPI 名；
- `ExtensionScanner` 扫描后把插件实现 **注册进宿主 `ExtensionLoader`**，宿主应用以统一 SPI 方式调用，不感知插件加载机制；
- 插件内扩展点（插件间互调）同样走 `@SPI`，不新增标注。

#### 4.8 context/ — 插件上下文（隔离的依赖注入）

##### 定位

插件运行环境与隔离的依赖注入。

```textmate
cn.jowen.framework.plugin.context
├─ DefaultPluginContext          #默认实现：configuration /sharedData /publishEvent
├─ PluginConfiguration           #插件配置：getString/getInt/getBoolean/getList/getMap/getAll
├─ SharedData                    #插件间共享数据：ConcurrentHashMap 实现，线程安全
├─ PluginSpringContextFactory    #Spring 子容器工厂（可选能力）
│   ├─ create：parent =宿主容器 →扫描插件@Configuration/@Component →refresh
│   └─ destroy：close 子容器
└─ PluginBeanPostProcessor       #自动注入 PluginContext /PluginConfiguration /SharedData
```

**Spring 子容器模型（可选能力）**：

```text
宿主 ApplicationContext（父容器）
├─ DataSource / CacheManager / MessageSource / 业务 Service
│
├─ Plugin A 子容器（parent = 宿主）
│   └─ @Component AlipayService（可注入父容器 Bean，父不可见子）
│
└─ Plugin B 子容器（parent = 宿主）
    └─ @Component WechatPayService

插件卸载：close 子容器 → 注销扩展点 → 关闭 PluginClassLoader → GC 回收
```

#### 4.9 event/ — 插件事件

##### 定位

插件生命周期与扩展变动的可观测通道（继承 framework-core Event）。

```textmate
cn.jowen.framework.plugin.event
├─ PluginEvent                  #基类：pluginId +timestamp +source
├─ PluginLoadedEvent /PluginStartingEvent /PluginStartedEvent
├─ PluginStoppingEvent /PluginStoppedEvent /PluginFailedEvent /PluginUnloadedEvent
├─ ExtensionRegisteredEvent /ExtensionUnregisteredEvent
└─ PluginEventListener          #监听器接口：

onEvent(PluginEvent)
```

#### 4.10 hotswap/ — 热部署支持

##### 定位

插件目录监听与自动部署。

```textmate
cn.jowen.framework.plugin.hotswap
├─ PluginHotSwapManager     #管理器：JDK WatchService 监听目录 → 新 jar 自动加载启动 /删除自动卸载 /更新自动重启
├─ HotSwapStrategy          #策略：RESTART（默认）/RELOAD_CLASSES /MANUAL
├─ PluginFileWatcher        #文件监听：onFileCreated /onFileModified /onFileDeleted +防抖（默认 3s）
└─ HotSwapContext           #上下文：oldVersion /newVersion /swapTime /success
```

#### 4.11 config/ — 配置属性

##### 定位

**不含自动装配类**（冲突修正决议 #2：`PluginAutoConfiguration` 及 Actuator 端点已上移 `framework-boot-autoconfigure`
，本层只留配置绑定）。

```textmate
cn.jowen.framework.plugin.config
└─ PluginProperties             #@ConfigurationProperties(prefix = "framework.plugin")
```

**装配约定**：
`PluginManager / PluginLifecycleManager / PluginRegistry / DependencyResolver / PluginHotSwapManager / PluginHealthIndicator / PluginEndpoint`
的实例化与条件判断全部由 `framework-boot-autoconfigure` 的 `PluginAutoConfiguration` 完成；Actuator 端点类物理位于
boot-autoconfigure（依赖 Spring Boot Actuator）。

#### 4.12 support/ — 通用工具与支撑类

##### 定位

工具与打包规范。

```textmate
cn.jowen.framework.plugin.support
├─ PluginUtils          #isValidPluginId /extractPluginJar /calculateChecksum /getPluginLibs
├─ PluginPackage        #打包规范：plugin.json +classes/+lib/（第三方依赖）
├─ ValidationError      #校验错误：field /message /severity
└─ AbstractPlugin       #插件抽象基类：descriptor /context /state +init/start/stop 模板
```

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────────────────────────────┐
│                     framework-plugin                              │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  config（装配层，Spring 桥接）              │  │
│  │  PluginAutoConfiguration ──→ PluginProperties             │  │
│  │  ├─ PluginManager / Registry / LifecycleManager          │  │
│  │  ├─ PluginHealthIndicator + PluginEndpoint（Actuator）    │  │
│  │  └─ PluginHotSwapManager + CommandLineRunner             │  │
│  └─────────────────────────┬─────────────────────────────────┘  │
│                            │ 创建                                 │
│  ┌─────────────────────────▼─────────────────────────────────┐  │
│  │                api（核心抽象层）                            │  │
│  │  Plugin ◄── PluginManager                                 │  │
│  │  PluginContext / PluginState                              │  │
│  └───────┬───────────────┬───────────────┬───────────────────┘  │
│          │               │               │                       │
│  ┌───────▼──────┐ ┌──────▼───────┐ ┌─────▼──────────┐        │
│  │  descriptor  │ │    loader    │ │   resolver     │        │
│  │  PluginDesc  │ │  PluginClass │ │  Dependency    │        │
│  │  PluginVer   │ │  Loader      │ │  Resolver      │        │
│  │  VersionRange│ │  SharedClass │ │  VersionArbit  │        │
│  │              │ │  Loader      │ │  rator / Graph │        │
│  └──────────────┘ └──────────────┘ └────────────────┘        │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │        lifecycle + registry + extension + context        │  │
│  │  LifecycleManager → StateTransition / HealthChecker      │  │
│  │  PluginRegistry → ExtensionRegistry → ExtensionPoint     │  │
│  │  @Extension → Scanner → Factory → DefaultPluginContext   │  │
│  │  PluginSpringContextFactory（可选子容器）                 │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │         hotswap（热部署）+ event（事件）+ support（工具）    │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  framework-core（SPI/Event/Lifecycle/Assert）             │  │
│  │  spring-context（可选，仅子容器模式）                     │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0（零内部依赖）    support（PluginUtils / PluginPackage / AbstractPlugin）
                      ▲
L1（依赖 L0）        api（Plugin/PluginManager/PluginContext）+ descriptor
                      ▲
L2（依赖 L1）        loader / resolver / event
                      ▲
L3（依赖 L0~L2）     lifecycle / registry / extension / context
                      ▲
L4（依赖 L0~L3）     hotswap / config（config 为唯一 Spring 依赖层）
```

**模块间规则**：

- 插件系统核心（api~hotswap）不依赖 Spring，可脱离 Spring 独立运行（单元测试 / 非 Spring 宿主）；
- `context.PluginSpringContextFactory` 依赖 `spring-context`（optional），仅在启用子容器时加载；
- `config` 依赖 `spring-boot-autoconfigure` 与 `spring-boot-starter-actuator`（optional）。

---

## 七、外部依赖

```xml

<dependencies>
    <!-- 框架内依赖 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-logger</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring 子容器（可选能力，核心不依赖） -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 自动装配（仅 config 层） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Actuator（健康检查 + 端点，可选） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- plugin.yaml 支持（可选） -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- JSpecify 空安全 -->
    <dependency>
        <groupId>org.jspecify</groupId>
        <artifactId>jspecify</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  plugin:
    enabled: true
    plugins-dir: ./plugins                  # 插件目录
    auto-start: true                        # 加载后自动启动
    disabled-plugins: # 禁用的插件
      - com.example.deprecated-plugin

    hot-swap:
      enabled: true
      strategy: restart                     # restart / reload-classes / manual
      debounce-interval: 3s

    class-loading:
      strategy: framework-api-delegate      # parent-first / child-first / framework-api-delegate
      exported-packages: # 宿主对外暴露的包
        - cn.jowen.framework.core.**
        - cn.jowen.framework.plugin.api.**
        - com.example.common.**
      hidden-classes: # 禁止插件加载
        - javax.servlet.**
      shared-libraries: # 共享库（避免插件间版本冲突）
        - com.fasterxml.jackson.core:jackson-databind
        - com.google.guava:guava

    spring: # 可选：Spring 子容器
      enabled: false                        # 默认关闭（保持插件系统轻量）
      parent-context-bean-visibility: true  # 子容器可见父容器 Bean

    health:
      enabled: true
      endpoint: /actuator/plugins
```

---

## 九、使用方式

#### 9.1 宿主定义扩展点

```textmate
// 扩展点声明复用 core @SPI（不另立 @ExtensionPoint）
@SPI("alipay")
public interface PaymentProvider {
    String getChannel();

    PaymentResult pay(PaymentRequest request);
}
```

#### 9.2 开发插件

```textmate
// plugin.json 声明扩展：{ "extensionPointId": "payment.provider", "className": "...AlipayPaymentProvider" }

@Extension(id = "alipay-provider", extensionPoint = "payment.provider", order = 10)
public class AlipayPaymentProvider implements PaymentProvider {
    @Override
    public String getChannel() {
        return "alipay";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) { /* 支付宝逻辑 */ }
}

// 插件主类
public class AlipayPlugin extends AbstractPlugin {
    @Override
    public void start(PluginContext context) {
        String appId = context.getConfiguration().getString("appId");
        // 初始化支付宝 SDK...
    }

    @Override
    public void stop() { /* 清理资源 */ }
}
```

#### 9.3 宿主使用插件扩展

```textmate

@Service
public class OrderService {
    @Autowired
    private PluginManager pluginManager;

    public PaymentResult pay(String channel, PaymentRequest request) {
        List<PaymentProvider> providers = pluginManager.getExtensions(PaymentProvider.class);
        PaymentProvider provider = providers.stream()
                .filter(p -> p.getChannel().equals(channel))
                .findFirst()
                .orElseThrow(() -> new BusinessException("不支持的支付渠道: " + channel));
        return provider.pay(request);
    }
}
```

#### 9.4 Actuator 端点

```bash
GET  /actuator/plugins                      # 查看所有插件状态
POST /actuator/plugins/{pluginId}/start     # 启动
POST /actuator/plugins/{pluginId}/stop      # 停止
POST /actuator/plugins/{pluginId}/restart   # 重启
```

---

## 十、SPI 扩展点汇总

| 扩展点接口               | 所在包     | 用途                                  |
|:-------------------------|:-----------|:--------------------------------------|
| `Plugin`                 | api        | 插件实现（推荐继承 `AbstractPlugin`） |
| `PluginDescriptorLoader` | descriptor | 自定义描述符加载方式                  |
| `PluginClassLoader`      | loader     | 自定义类加载策略                      |
| `DependencyResolver`     | resolver   | 自定义依赖解析算法                    |
| `ExtensionFactory`       | extension  | 自定义扩展实例化方式                  |
| `PluginEventListener`    | event      | 自定义插件事件监听                    |
| `PluginHealthChecker`    | lifecycle  | 自定义插件健康检查                    |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│                    业务应用（Application）                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│           framework-boot（适配编排层）                        │
│  PluginAutoConfiguration（framework.plugin.enabled）        │
│  ├─ PluginManager / Registry / LifecycleManager            │
│  ├─ PluginHealthIndicator + PluginEndpoint → Actuator       │
│  └─ CommandLineRunner → ApplicationReady 后加载插件         │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-plugin（插件系统，零 Spring 核心）     │
│  PluginManager / PluginClassLoader / LifecycleManager       │
│  ExtensionRegistry / DependencyResolver / HotSwapManager    │
└──────────────────────────┬──────────────────────────────────┘
                           │ 按需
┌──────────────────────────▼──────────────────────────────────┐
│  framework-core（SPI/Event）· spring-context（可选子容器）   │
│  Spring Boot 4.x（@AutoConfiguration / Actuator）            │
└─────────────────────────────────────────────────────────────┘
```

**依赖方向**：业务 → plugin（API/注解）→ core（基础设施）；Spring 桥接全部收敛在 config 子包与 boot 层。插件 jar 与宿主仅通过
`cn.jowen.framework.core.**`、`cn.jowen.framework.plugin.api.**` 及宿主导出的公共包交互，实现双向隔离。
