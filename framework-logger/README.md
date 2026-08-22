# framework-logger 模块架构设计

> 文档元信息
> - **模块**：framework-logger
> - **关键词**：日志门面、日志脱敏、链路追踪、结构化日志、异步日志
> - **描述**：统一日志门面，提供日志脱敏、链路追踪增强（虚拟线程适配）、异步日志、结构化日志能力
> - **基线**：Spring Boot 4.x + Java 21（Micrometer 2.0 ContextSnapshot、GraalVM RuntimeHints）

---

## 一、模块定位

`framework-logger` 是框架的 **日志管理模块（L1）**，为业务与框架各模块提供统一日志门面，屏蔽 Logback/Log4j2 实现差异，并叠加
**日志脱敏、链路追踪增强、结构化日志、异步日志** 能力。

**核心价值**：

| 场景         | 没有本模块                         | 有本模块                            |
|:-------------|:-----------------------------------|:------------------------------------|
| 日志实现切换 | 业务代码写死 Logback/Log4j2 API    | 门面隔离，一行配置切换              |
| 敏感信息     | 手机号/身份证明文入日志            | 内置 MaskPattern 自动脱敏           |
| 链路追踪     | 多线程/虚拟线程切换后 TraceId 丢失 | ContextSnapshot 传播，MDC 不丢      |
| 日志检索     | 非结构化文本难以解析               | JSON 结构化输出（JsonLogFormatter） |

**与核心模块的边界**：

| 模块                 | 定位              | 特点                                |
|:---------------------|:------------------|:------------------------------------|
| **framework-logger** | **日志门面+增强** | **脱敏/追踪/结构化**                |
| framework-core       | 基础设施          | SPI、异常、断言（logger 依赖）      |
| framework-extras     | 工具集            | operatelog 操作日志复用 logger 门面 |

---

## 二、功能清单与依赖矩阵

| 功能         | 子包    | 核心依赖                 | 可选依赖         |
|:-------------|:--------|:-------------------------|:-----------------|
| 日志门面     | facade  | framework-core           | —                |
| 实现适配     | adapter | —                        | Logback / Log4j2 |
| 链路追踪增强 | trace   | facade                   | Micrometer 2.0   |
| 日志脱敏     | mask    | core（desensitize 内核） | —                |
| 结构化日志   | layout  | facade                   | Jackson 3        |
| 配置         | config  | facade                   | —                |

---

## 三、整体包结构

```text
framework-logger
└─ src/main/java/com/framework/logger/
   ├─ facade/                 # 日志门面（Logger / LoggerFactory / LogLevel）
   ├─ adapter/                # 日志实现适配（Logback / Log4j2）
   ├─ trace/                  # 链路追踪增强（TraceEnhancer / MdcContextPropagation）
   ├─ mask/                   # 日志脱敏（core.desensitize 适配：LogMasker / LogMaskLayout）
   ├─ layout/                 # 结构化日志（StructuredLayout / JsonLogFormatter）
   └─ config/                 # 配置（LoggerProperties）
```

---

## 四、各子包详细设计

#### 4.1 facade/ — 日志门面

##### 定位

业务只面向统一 `Logger` 接口，不感知底层实现。

```textmate
cn.jowen.framework.logger.facade
├─ Logger                    #日志接口：info/warn/error/debug +占位符
├─ LoggerFactory             #工厂：LoggerFactory.getLogger(Class/name)
└─ LogLevel                  #枚举：TRACE/DEBUG/INFO/WARN/ERROR
```

#### 4.2 adapter/ — 实现适配

##### 定位

桥接门面与底层日志实现，支持运行时切换。

```textmate
cn.jowen.framework.logger.adapter
├─ LoggerAdapter             #适配器接口
├─ LogbackAdapter            #Logback 适配（默认）
└─ Log4j2Adapter             #Log4j2 适配
```

#### 4.3 trace/ — 链路追踪增强

##### 定位

TraceId 注入与跨线程/虚拟线程传播，保证日志链路完整。

```textmate
cn.jowen.framework.logger.trace
├─ TraceEnhancer             #TraceId 注入（无侵入）
├─ TraceContext              #追踪上下文（traceId/spanId），基于 core  ContextCarrier
└─ MdcContextPropagation     #MDC ↔ ContextCarrier 桥接（虚拟线程适配）
```

**上下文统一（冲突修正决议 #3）**：`TraceContext` 读写委托 `core.context.ContextCarrier`（默认 ScopedValue，兼容模式 ThreadLocal），不再自建；虚拟线程之间迁移用 `ContextSnapshot.capture()/replay()`。

```textmate
// MDC ↔ ContextCarrier 双向桥接（经 ContextPropagator SPI 注册）
public class MdcContextPropagation implements ContextPropagator {
    @Override
    public Runnable wrap(Runnable task) {
        ContextSnapshot snapshot = ContextSnapshot.capture();   // 统一快照
        return () -> snapshot.replay(task);                      // 跨虚拟线程迁移
    }
    // 可选：适配 Micrometer ContextSnapshot（io.micrometer.context）作为桥接实现之一
}
```

#### 4.4 mask/ — 日志脱敏（core.desensitize 场景适配）

##### 定位

**日志场景适配层，不实现规则**。规则模型/执行器统一来自 `framework-core.desensitize`，本子包只负责"何时对日志消息脱敏"。

```textmate
cn.jowen.framework.logger.mask
├─ LogMasker                 #日志脱敏入口：委托 core Desensitizer，按 LoggerProperties 开关
└─ LogMaskLayout             #Logback/ Log4j2 布局装饰器：输出前对消息做脱敏
```

**与 core 的分工**：

- 内置策略（手机号/身份证/银行卡/邮箱…）→ `core.desensitize.DesensitizeStrategies`， **本模块不复制**；
- 自定义规则 → 实现 `core.desensitize.DesensitizeRule` 并经 SPI 注册，日志与结果集/JSON 输出 **一处定义、处处生效**；
- 本模块只注册 `LogMaskLayout`（把脱敏挂在日志布局/Appender 上）。

**示例**：`13812341234` → `138****1234`；`110101199001011234` → `110***********1234`（策略来自 core 内置集）。

**配置**：`framework.desensitize.enabled=true`（全局统一开关，替代原 `logger.mask-enabled`）。

#### 4.5 layout/ — 结构化日志

##### 定位

输出 JSON 结构化日志，便于采集检索。

```textmate
cn.jowen.framework.logger.layout
├─ StructuredLayout          #结构化布局
└─ JsonLogFormatter          #JSON 输出（Jackson 3JsonMapper）
```

#### 4.6 config/ — 配置

```textmate
cn.jowen.framework.logger.config
└─ LoggerProperties          #mask-enabled /async-enabled /level...
```

---

## 五、核心类关系图

```text
┌──────────────────────────────────────────────────────────────┐
│                    framework-logger                           │
│                                                              │
│  业务代码 ──→ LoggerFactory ──→ Logger（facade）              │
│                                  │                           │
│                    ┌─────────────┼─────────────┐             │
│                    ▼             ▼             ▼             │
│              LogbackAdapter   Log4j2Adapter  layout           │
│              (默认)           (可选)         JsonLogFormatter │
│                    ▲                           ▲             │
│                    │ 注入                       │ 脱敏         │
│              trace（MdcContextPropagation）  mask（Masker）   │
│              ⚡ ContextSnapshot 虚拟线程传播                   │
│                                                              │
│  依赖：framework-core（Assert/SPI）· Micrometer 2.0           │
│  GraalVM：LoggerRuntimeHints 注册反射                        │
└──────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0     framework-core
  ▲
L1     framework-logger（本模块）
  ▲
L2     data-jdbc / cache / i18n / extras（使用日志门面）
```

---

## 七、外部依赖

```xml

<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-slf4j-impl</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-core</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  logger:
    enabled: true
    async-enabled: true         # 异步日志（虚拟线程消费）
    level: info
    format: json                # text / json
  desensitize:
    enabled: true               # 脱敏全局开关（core 统一，日志/结果集/JSON 共用）
```

---

## 九、使用方式

```textmate
// 门面使用（业务无感底层实现）
Logger log = LoggerFactory.getLogger(UserService.class);
log.info("用户创建成功, userId={}",userId);          // 自动脱敏 + TraceId

// 编程式设置 TraceId
try(
    var ignored = TraceContext.open()){
    log.info("业务执行");   // 自动携带 traceId/spanId
}
```

---

## 十、SPI 扩展点汇总

| 扩展点接口                            | 所在包  | 用途                                                  |
|:--------------------------------------|:--------|:------------------------------------------------------|
| `LoggerAdapter`                       | adapter | 接入新日志实现                                        |
| `ContextPropagator`（core.context）   | trace   | MDC 桥接 / 自定义上下文传播（跨虚拟线程）             |
| `DesensitizeRule`（core.desensitize） | mask    | 自定义脱敏规则（日志/结果集/JSON 共用，见 core 模块） |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│              framework-boot (适配编排层)                    │
│  └─ LoggerAutoConfiguration                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-logger（本模块）                     │
│  facade / adapter / trace / mask / layout / config          │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              framework-core（基础工具 / SPI）               │
└─────────────────────────────────────────────────────────────┘
```

**Spring Boot 4.x 适配要点**：MDC 用 Micrometer ContextSnapshot 传播（虚拟线程切换不丢）；提供 `LoggerRuntimeHints`（GraalVM 反射注册）；异步日志消费线程用虚拟线程。
