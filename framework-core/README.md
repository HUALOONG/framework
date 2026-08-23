# framework-core 模块架构设计

> 文档元信息
> - **模块**：framework-core
> - **关键词**：SPI、异常体系、断言、生命周期、事件机制、零依赖
> - **描述**：框架基础设施层，提供 SPI 扩展机制、异常体系、断言工具、生命周期管理与事件机制；唯一外部依赖 JSpecify
> - **基线**：Spring Boot 4.x + Java 21（core 层对 Spring **零依赖**）

---

## 一、模块定位

`framework-core` 是框架的 **基础设施层（L0）**，为所有上层模块提供 SPI 扩展机制、统一异常体系、断言工具、生命周期管理和事件机制。
**该层保持纯净——仅 JDK 21 + JSpecify，不依赖 Spring 与任何第三方库**，可脱离容器做单元测试。

**核心价值**：

| 场景         | 没有本模块                 | 有本模块                           |
| :----------- | :------------------------- | :--------------------------------- |
| 框架能力扩展 | 硬编码、改框架源码         | SPI + @Activate 按需激活扩展       |
| 异常处理     | 各模块自定义异常，风格不一 | 统一异常体系 + 错误码规范          |
| 参数校验     | 手写 if/throw              | Assert / State 链式断言            |
| 组件启停     | 依赖 Spring 生命周期       | 自研 Lifecycle 抽象，可脱离 Spring |

**与核心模块的边界**：

| 模块                | 定位         | 特点                        |
| :------------------ | :----------- | :-------------------------- |
| **framework-core**  | **基础设施** | **零依赖、SPI、异常、断言** |
| framework-logger    | 日志管理     | 依赖 core，提供日志门面     |
| framework-data-core | 数据抽象     | 依赖 core，提供数据访问抽象 |

---

## 二、功能清单与依赖矩阵

| 功能                         | 子包        | 核心依赖 | 可选依赖 |
| :--------------------------- | :---------- | :------- | :------- |
| SPI 扩展机制                 | spi         | —        | —        |
| 异常体系                     | exception   | —        | —        |
| 生命周期管理                 | lifecycle   | —        | —        |
| 断言工具                     | assertion   | —        | —        |
| 事件机制                     | event       | —        | —        |
| 通用工具                     | util        | —        | —        |
| 数据脱敏内核                 | desensitize | —        | —        |
| 上下文传播（ContextCarrier） | context     | —        | —        |

---

## 三、整体包结构

```text
framework-core
└─ src/main/java/cn/jowen/framework/core/
   ├─ assertion/              # 断言工具
   ├─ context/                # 上下文传播（ContextCarrier，ScopedValue/ThreadLocal）
   ├─ desensitize/            # 数据脱敏内核（规则模型+执行器+注解）
   ├─ event/                  # 事件机制
   ├─ exception/              # 异常体系
   ├─ lifecycle/              # 生命周期
   ├─ spi/                    # SPI 扩展机制
   └─ util/                   # 通用工具（不含脱敏，见 desensitize）
```

---

## 四、各子包详细设计

#### 4.1 spi/ — SPI 扩展机制

##### 定位

框架级服务加载机制，支持按接口发现实现、条件激活、排序。

```textmate
cn.jowen.framework.core.spi
├─ ExtensionLoader<T>              # SPI 加载器：load(Class<T> type) / getExtension(name)
├─ SPI                             # @SPI 标注扩展点接口（name 默认实现名）
└─ Activate                        # @Activate 条件激活（group/order）
```

**使用示例**：

```textmate
// 定义扩展点
@SPI("jdbc")
public interface DialectProvider { String dialect(); }

// 加载实现
ExtensionLoader<DialectProvider> loader = ExtensionLoader.get(DialectProvider.class);
DialectProvider provider = loader.getDefaultExtension();
```

#### 4.2 exception/ — 异常体系

##### 定位

统一框架异常树与错误码规范，业务/系统/框架异常三分类。

```textmate
cn.jowen.framework.core.exception
├─ FrameworkException              # 框架异常根类（含 ErrorCode）
├─ BusinessException               # 业务异常（message + code 可前端展示）
├─ SystemException                 # 系统异常（内部错误，不对外暴露细节）
└─ ErrorCode                       # 错误码接口（code() + message()）
```

#### 4.3 lifecycle/ — 生命周期管理

##### 定位

框架组件统一的启动/停止生命周期抽象，支持排序与依赖。

```textmate
cn.jowen.framework.core.lifecycle
├─ Lifecycle                       # 生命周期接口：start() / stop() / isRunning()
├─ SmartLifecycle                  # 智能生命周期：getPhase() 排序
└─ LifecycleProcessor              # 生命周期处理器：管理一组 Lifecycle
```

#### 4.4 assertion/ — 断言工具

##### 定位

参数与状态校验，失败抛出带错误码的异常。

```textmate
cn.jowen.framework.core.assertion
├─ Assert                          # 参数断言：notNull / hasText / isTrue / notEmpty...
└─ State                           # 状态断言：checkState / checkNotNull...
```

#### 4.5 event/ — 事件机制

##### 定位

轻量事件发布订阅，供框架内部与插件系统使用。

```textmate
cn.jowen.framework.core.event
├─ Event                            # 事件基类（含 timestamp/source）
└─ EventListener<E extends Event>   # 监听器接口：onEvent(E event)
```

#### 4.6 util/ — 通用工具

##### 定位

类加载、字符串、集合、反射工具（Java 21 优化：优先 MethodHandles）。

```textmate
cn.jowen.framework.core.util
├─ ClassUtils        # 类加载/扫描
├─ StringUtils       # 空判断/占位符/简单字符串处理（不含脱敏）
├─ CollectionUtils   # 集合操作
└─ ReflectionUtils   # 反射内省（MethodHandles 优先）
```

#### 4.7 desensitize/ — 数据脱敏内核（全框架唯一脱敏实现）

##### 定位

**全框架唯一脱敏规则模型与执行器**。logger.mask（日志脱敏）、data-mybatis `@Mask`（结果集脱敏）、extras.desensitize（JSON 输出脱敏）、data-jdbc SQL 脱敏 **全部只做场景适配，不再各自实现规则**。。

```textmate
cn.jowen.framework.core.desensitize
├─ DesensitizeRule          # 规则接口：strategy() + apply(String raw, DesensitizeContext ctx)
├─ DesensitizeStrategies    # 内置策略：PHONE / ID_CARD / BANK_CARD / EMAIL / NAME / ADDRESS / PASSWORD / FIXED_PHONE / LICENSE_PLATE / CUSTOM
├─ Desensitizer             # 执行器：register(rule) / mask(String, strategy) / maskObject(Object, @DesensitizeField 注解) / maskMap(Map)
├─ DesensitizeContext       # 执行上下文：startKeep / endKeep / replacement / skip(临时跳过，管理员查看原始数据)
├─ DesensitizeField         # @DesensitizeField 字段注解：strategy + startKeep + endKeep + replacement
└─ DesensitizeException     # 脱敏配置错误异常（复用 exception 树）
```

**设计要点**：

1. **规则与场景解耦**：规则只回答"怎么脱"，场景适配层回答"何时对谁脱"（日志 → 结果集 → JSON 输出）。
2. **注解统一**：各模块适配注解（`@Mask` / `@Desensitize`）声明 `strategy` 后 **委托 `Desensitizer` 执行**，不复制规则实现。
3. **SPI 扩展**：`DesensitizeRule` 实现类经 `ExtensionLoader` 注册，业务自定义策略全局生效（日志/结果集/JSON 一处定义、处处生效）。
4. **线程安全**：手动注册规则集合使用 `Collections.synchronizedList` 保证线程安全；SPI 发现的规则不可变，读写分离。

**Java 21 适配**：内置策略为不可变 enum，策略校验与脱敏逻辑均在策略枚举内部完成。

#### 4.8 context/ — 上下文传播（ContextCarrier）

##### 定位

**全框架唯一上下文传播机制**。统一 i18n（Locale）、data-mybatis（多租户/数据权限/脱敏）、extras（DataPermission/OperateLog）、data-core（DataSourceContext）、logger（TraceId）的上下文读写。

```textmate
cn.jowen.framework.core.context
├─ ContextCarrier          # 上下文载体：方案A ScopedValue（默认）/ 方案B ThreadLocal（兼容模式）
├─ ContextKey<T>           # 上下文键：全局唯一（名称+类型），如 ContextKey.named("tenantId", String.class)
├─ ContextSnapshot         # 快照：capture() / replay(Runnable) / scopedRun(key, value, task) —— 跨虚拟线程迁移
└─ ContextPropagator       # 传播器 SPI：可注册 AsyncExecutor 桥接（线程池/虚拟线程调度时自动携带快照）
```

**设计要点**：

1. **方案 A（默认，Java 21 ScopedValue）**：`ScopedValue.where(KEY, v).run(task)`，虚拟线程下零开销、隔离性最佳；
   `ContextSnapshot.replay` 封装 `runWithSnapshot` 迁移语义。
2. **方案 B（兼容，ThreadLocal）**：`framework.context.mode=threadlocal` 切换，继承式传递，用于遗留线程池/中间件强制
   ThreadLocal 场景（如 MyBatis 老版本拦截器）。
3. **两方案 API 同形**：`ContextCarrier.get(key) / set(key, v) / runWith(key, v, task)`，业务代码不感知模式差异。
4. **快照迁移**：虚拟线程之间传递上下文必须显式 `capture → replay`，禁止隐式全局状态。

**配置**：`framework.context.mode=scopedvalue|threadlocal`（默认 `scopedvalue`）。

---

## 五、核心类关系图

```text
┌──────────────────────────────────────────────────────────┐
│                    framework-core                        │
│                                                          │
│  spi (ExtensionLoader) ──→ 加载 ──→ 各模块扩展实现       │
│                                                          │
│  exception (FrameworkException)                          │
│     ├─ BusinessException / SystemException               │
│     └─ ErrorCode                                         │
│                                                          │
│  lifecycle (Lifecycle / SmartLifecycle)                  │
│     └─ LifecycleProcessor                                │
│                                                          │
│  assertion (Assert / State) ──→ 抛出 exception           │
│                                                          │
│  event (Event / EventListener)                           │
│                                                          │
│  util (Class/String/Collection/Reflection Utils)         │
│                                                          │
│  desensitize (DesensitizeRule / Desensitizer)            │
│     └─ 各模块适配层（logger.mask / @Mask / @Desensitize）│
│                                                          │
│  context (ContextCarrier / ContextSnapshot)              │
│     └─ 各模块上下文（i18n / 多租户 / 数据权限 / trace）  │
│                                                          │
│  ⚠ 唯一依赖：JSpecify（@NullMarked 空安全）             │
└──────────────────────────────────────────────────────────┘

---

## 六、分层依赖规则

```text
L0（零依赖）     framework-core
  ▲
L1               framework-logger / framework-data-core
  ▲
L2               framework-data-jdbc / mybatis / cache / i18n
  ▲
L3               framework-boot-autoconfigure / extras / plugin
```

---

## 七、外部依赖

```xml
<dependencies>
    <!-- 唯一外部依赖：JSpecify 空安全注解 -->
    <dependency>
        <groupId>org.jspecify</groupId>
        <artifactId>jspecify</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

编译配置：`<maven.compiler.release>21</maven.compiler.release>`。

---

## 八、配置属性

core 层本身不读取配置；唯一例外是 `context` 子包的模式选择（由 boot-autoconfigure 注入，不直接读配置文件）：

| 键                       | 默认值        | 说明                                                                    |
| :----------------------- | :------------ | :---------------------------------------------------------------------- |
| `framework.context.mode` | `scopedvalue` | 上下文模式：`scopedvalue`（Java 21 ScopedValue）/ `threadlocal`（兼容） |

---

## 九、使用方式

```textmate
// 断言
Assert.notNull(user, "用户不能为空", ErrorCode.PARAM_ERROR);
State.checkState(cache.isRunning(), "缓存未启动");

// 事件
eventBus.publish(new ResourceReloadedEvent(this, path));

// SPI
ExtensionLoader<DataPermissionRule> loader = ExtensionLoader.get(DataPermissionRule.class);
```

---

## 十、SPI 扩展点汇总

| 扩展点接口                     | 所在包      | 用途                                                      |
| :----------------------------- | :---------- | :-------------------------------------------------------- |
| `ExtensionLoader<T>`           | spi         | 框架级服务发现（各模块实现复用）                          |
| `ErrorCode`                    | exception   | 自定义错误码枚举                                          |
| `Lifecycle` / `SmartLifecycle` | lifecycle   | 自定义组件生命周期                                        |
| `DesensitizeRule`              | desensitize | 自定义脱敏规则（日志/结果集/JSON 输出一处定义、处处生效） |
| `ContextPropagator`            | context     | 自定义上下文跨执行器传播桥接（线程池/虚拟线程调度）       |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────┐
│              framework-boot (适配编排层)                │
│  └─ boot-autoconfigure / boot-starter                   │
└──────────────────────────┬──────────────────────────────┘
                           │ 依赖
┌──────────────────────────▼──────────────────────────────┐
│  framework-logger · data-core · data-jdbc · mybatis     │
│  cache · i18n · plugin · extras                         │
└──────────────────────────┬──────────────────────────────┘
                           │ 唯一地基
┌──────────────────────────▼──────────────────────────────┐
│            framework-core（L0 零依赖地基）              │
│  SPI / Exception / Lifecycle / Assert / Event / Util    │
└─────────────────────────────────────────────────────────┘
```

**Spring Boot 4.x/Java 21 适配**：包级 `@NullMarked` 空安全标记；编译目标 21。
