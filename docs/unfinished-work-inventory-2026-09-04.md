# Jowen Framework 未完成项全量清单（2026-09-04）

> **审计方式**：主理人只读扫描（团队成员因 API 429 限流全部失败，未产生结论；本次由主理人直接基于工具链完成一手核实，与 09-01 `unfinished-work-audit-2026-09-01.md` 的结论交叉比对）
> **技术栈**：Spring Boot 4.1.0 + Java 21 + Maven 多模块
> **审计基线**：工作区当前状态（含未提交变更 230 处）+ HEAD 已提交版本对照
> **扫描范围**：全部 10 个 Maven 模块（bom / core / data×3 / cache / logger / i18n / extras×3 / plugin / boot×3 / coverage）+ 构建配置 + README + docs/

---

## 一、总体结论

1. **旧审计 9 个 P0 缺陷已 100% 修复**（含 5 项已提交 + 3 项工作区重构中 + 1 项工具链达成）。
2. **新增 1 个 P0 级装配缺口**（AutoConfiguration.imports 漏登记 3 个类，导致 3 组能力生产环境整体失效，测试覆盖率 95% 仍为绿色——**测试盲区典型**）。
3. **未完成项从"缺陷密度"转为"契约密度"**：剩余问题不再是 P0 级静默错误，主要是空接口、抽象骨架、单机化存储、运行时契约引导。
4. **主流程阻塞项**：data-mybatis 工作区大规模重构**尚未提交**，是主流程推进的唯一硬阻塞。
5. **覆盖率主目标达成**：JaCoCo 聚合行覆盖 95.26%（8027/8426），19/19 模块 BUILD SUCCESS，160 测试 0 失败。

---

## 二、未完成项清单（按优先级排序）

| 编号 | 模块路径 | 功能名称 | 当前状态 | 缺失内容描述 | 阻塞依赖 | 优先级 |
|---|---|---|---|---|---|---|
| **P0-001** | `framework-boot/framework-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 自动配置登记 | **部分实现** | 全仓仅 1 个 imports 文件，登记 11 个 `@AutoConfiguration`，漏登 3 个：`FileStorageAutoConfiguration`、`MessageAutoConfiguration`、`WebExtrasAutoConfiguration`。已确认无第二个 `.imports` 文件、无 `spring.factories`、生产代码零引用——仅测试用 `AutoConfigurations.of(...)` 显式加载 | 无 | **P0** |
| **P0-002** | `framework-data/framework-data-mybatis/` | 仓储+扩展点重构 | **部分实现（未提交）** | 工作区已删 11 个源文件（FlexJoinRepository/ConditionMapper/FlexLambdaQueryBuilder/FlexQueryWrapperTranslator/FlexDynamicRepository/FlexRepository/FlexRepositoryFactory/IdGeneratorAdapter/FlexRepositoryAdapter/FlexTransactionAdapter）+ query 包整包，新增 `Encrypted` 注解、重写 `ExtensionRegistry`（fire 链完整实现）、重构 `FlexEncryptProcessor`（decrypt 走注解反射）。**变更未提交**，是主流程推进的硬阻塞 | 无 | **P0** |
| **P1-001** | `framework-cache/src/main/java/.../cache/multilevel/CacheSyncListener.java` | 跨节点缓存同步 | **未开始** | 接口已定义（`onCachePut` / `onCacheEvict`），但全仓**无任何实现类**。与 README §10.3 "多级缓存命中率可观测"存在落差 | 依赖 Redis pub/sub 或 Redisson topic 选择 | **P1** |
| **P1-002** | `framework-plugin/src/main/java/.../plugin/api/PluginManager.java:45`<br>`.../plugin/lifecycle/PluginLifecycleManager.java:180,185` | 插件生命周期 | **部分实现** | 3 处 `UnsupportedOperationException`：`initialize`（L45）、`loadPlugins`（L180）、`loadPlugin`（L185）。属于接口默认方法引导用户走正确 API，但作为对外契约属于未完成 | 无 | **P1** |
| **P1-003** | `framework-data/framework-data-mybatis/src/main/java/.../extension/FlexEncryptProcessor.java:163` | 加解密策略 | **部分实现** | 内嵌 `Decrypt` 接口 `default String decrypt(String)` 抛 `UnsupportedOperationException("decrypt not implemented")`，需具体策略类补齐（ECB/AES 实现选择待定） | 无 | **P1** |
| **P1-004** | `framework-boot/framework-boot-autoconfigure/src/main/java/.../extras/WebExtrasAutoConfiguration.java:140` | 分布式 Web 增强 | **待联调** | `new HashMap<String, byte[]>` 存 keys——幂等/限流/加密密钥/验证码在多实例部署下无法跨节点生效。与 README §10.3 的分布式能力承诺存在落差 | 依赖 CacheSyncListener（P1-001）实现完成 | **P1** |
| **P1-005** | `framework-data/framework-data-jdbc/src/main/java/.../transaction/` | 事务传播级别 | **未开始** | 目录内 grep 无 `NESTED` / `SAVEPOINT` 匹配，事务传播级别不完整（缺 NESTED 支持，即无保存点回滚） | 无 | **P1** |
| **P1-006** | `framework-data/framework-data-jdbc/` | SQL 联表查询 | **未开始** | 全模块 grep 无 `join` / `JoinType` 匹配，data-jdbc 侧无 join 能力。README §8.4 声称的能力存在落差 | 依赖 data-core QueryWrapper 重构完成（工作区已删） | **P1** |
| **P2-001** | `framework-extras/framework-extras-message/src/main/java/.../provider/*.java` | 消息通道 | **部分实现** | 7 个 sender（Email/DingTalk/Push/Site/Sms/Webhook/WeCom）全部是 41~57 行的 `abstract class`，`doSend` 需下游实现。README 声称"开箱即用"存在落差 | 无（设计上就是模板方法） | **P2** |
| **P2-002** | 全模块 | GraalVM AOT RuntimeHints | **部分实现** | 全仓 `RuntimeHintsRegistrar` 仅 6 个，全部集中在 `framework-boot-autoconfigure/src/main/java/.../runtime/`（Cache/I18n/Jdbc/Logger/Mybatis/Spi）。README §5/§8 要求"每模块提供 RuntimeHintsRegistrar"未落地（core/extras/plugin 无独立 registrar） | 无 | **P2** |
| **P2-003** | `framework-data/framework-data-core/` | QueryWrapper 重构 | **待测试** | `query/QueryWrapper.java` 在工作区已删除（HEAD 版本存在，且旧审计指出 `having()` 是伪实现——塞进 groupBy 列表产生非法 SQL）。重构方向未明 | 需补充上下文：重构目标、替代方案 | **P2** |
| **P2-004** | `docs/code-analysis.md` / `README.md` 若干章节 | 文档一致性 | **部分实现** | 09-01 审计已指出：① README §8.9 称 extras 含 4 子模块（实际 3，web 在 boot-web）；② boot-web README 标题仍是"framework-extras-web"且错误声称"基于 framework-cache 存储底座"；③ extras-message README §4.2 列 4 个发送器（实际 7 个）；④ code-analysis.md 行号引用/EventBus 行为/7 大风险状态全部过时。已有一次 README 更新（08-28 commit）清理 i18n Redis/AOT/指标过时标记 | 无 | **P2** |

---

## 三、表 B — 测试缺口清单

| 模块 | 被测类 | 测试状态 | 缺失内容 | 优先级 |
|---|---|---|---|---|
| framework-extras-storage | `MinioFileStorage` / `OssFileStorage` / `S3FileStorage` / `S3Clients` | 有测试但**仅 mock 单测**，从未对真实服务联调 | 缺少 MinIO/OSS/S3 真实实例集成测试（`@Disabled` 集成测试或 Testcontainers 用例） | P1 |
| framework-extras-message | 7 个 abstract sender | **无测试**（abstract class 无法实例化测试） | 需要下游具体实现类后配套测试；当前抽象骨架的 send 包装逻辑无单元测试 | P2 |
| framework-plugin | `PluginLifecycleManager.loadPlugins` / `loadPlugin` / `PluginManager.initialize` | 测试会抛 `UnsupportedOperationException`，无正路径 | 需要明确契约后补齐测试（哪些用例应该成功、哪些应该抛） | P1 |
| framework-cache | `CacheSyncListener` | **无测试**（无实现类） | 依赖 P1-001 完成后配套测试 | P1 |
| framework-data-jdbc | transaction 目录全类 | 无 NESTED 传播级别用例 | 需先实现 NESTED 再补测试（TDD 建议） | P1 |
| framework-data-mybatis | 重构后 `ExtensionRegistry.firePreSave/firePostSave/firePreUpdate/firePostUpdate/firePostDelete/firePostLoad` 6 个方法 | 有 `ExtensionRegistryTest` 已修改但工作区未提交 | 需在重构提交后回归 | P0（随 P0-002 一起） |
| framework-boot-autoconfigure | `FileStorageAutoConfiguration` / `MessageAutoConfiguration` / `WebExtrasAutoConfiguration` | 测试用 `AutoConfigurations.of(...)` 显式加载，**未走 imports 注册机制** | 测试未覆盖 P0-001 缺陷；建议新增"从 `spring.factories` 加载全部装配类"的元测试 | **P0** |

---

## 四、已完成项（与 09-01 审计的 P0 对照）

| 09-01 编号 | 问题 | 当前状态 |
|---|---|---|
| P0-A | 覆盖率工具链断裂（无 prepare-agent、无 jacoco:check、coverage 未入 reactor） | ✅ **已修复**：coverage 入 reactor、prepare-agent 已配置、check 阈值 ≥0.95 |
| P0-B | 覆盖率门禁规则缺失 | ✅ **已修复**：`jacoco:check` 硬编码 0.95，全绿 |
| P0-C-① | FlexJoinRepository 联表回退全表 | ✅ **已修复**（重构删除文件） |
| P0-C-② | ConditionMapper WHERE 字符串拼接 | ✅ **已修复**（重构删除文件） |
| P0-C-③ | ExtensionRegistry fire 链未接通（4 项能力失效） | ✅ **已修复**（工作区重写，fire 链完整实现，但**未提交**） |
| P0-D | MultilevelCache NullValue 泄漏 | ✅ **已修复**：`get()` 内 `if (nullValueCache && isNullValue(localVal)) return null;` |

---

## 五、模块完成度估算

覆盖率数据源：`DELIVERY-REPORT-coverage-95pct.md`（终态）+ 本次工作区实测。**注意**：`coverage-gap-report.md` 是 09-02 的旧数据（83.82%），**不是当前值**。

| 模块 | main 文件 | test 文件 | 测试覆盖类占比 | 完成度估算 | 依据 |
|---|---:|---:|---|---:|---|
| framework-core | 45 | 24 | 中 | **90%** | EventBus/ContextCarrier/ExtensionLoader 完整，仅个别分支盲区 |
| framework-logger | 20 | 11 | 中 | **90%** | 脱敏 LogMasker/Layout 已实现，异步写入链路完整 |
| framework-i18n | 67 | 29 | 中 | **88%** | Redis 热加载/指标/Micrometer 全部已实现（旧审计说缺失是**过时的**），RedisMessageSource/Watcher 完整 |
| framework-cache | 53 | 22 | 中 | **85%** | 多级缓存/NullValue/Redisson 已修好，唯 CacheSyncListener 空接口 |
| framework-data-core | 43 | — | 中 | **80%** | 重构中（QueryWrapper 已删），映射/分页/排序/dialect 完整 |
| framework-data-jdbc | 51 | — | 中 | **75%** | 缺 NESTED、缺 join、TenantInterceptor 拼 SQL 末尾（旧审计 P1-3 未确认已修） |
| framework-data-mybatis | 21 | 6 | 低 | **60%**（工作区）<br>45%（HEAD） | 大规模重构未提交；HEAD 版本 P0-C 三项存在；工作区已修好 |
| framework-extras-common | 35 | — | 中 | **85%** | Snowflake 已修同毫秒冲突、IpRegion 已实现，异常体系完整 |
| framework-extras-message | 21 | — | 低 | **65%** | 7 个 sender 都是 abstract 骨架，未对真实服务联调 |
| framework-extras-storage | 18 | — | 中 | **85%** | Local/Minio/Oss/S3 4 种实现完整，仅缺真实服务联调 |
| framework-plugin | 81 | 56 | 高 | **85%** | 三处 USO 是引导性设计，热部署/hotswap 完整 |
| framework-boot-autoconfigure | 主装配 | 高 | 高 | **75%** | **P0-001 装配漏登记 3 类**是主流程阻断，装配分支盲区 |
| framework-boot-web | 小 | 中 | 中 | **80%** | 单实例化存储是分布式部署的短板 |
| framework-boot-starter | 小 | 中 | 中 | **90%** | 聚合入口干净 |
| framework-bom | — | — | — | **90%** | 已纳管 14 个内部模块，漏管 framework-coverage（聚合模块无需 BOM 管理，**不算问题**） |
| framework-coverage | — | — | — | **100%** | 门禁达标 95.26% |

---

## 六、未完成项总数与推进顺序

### 汇总统计

- **未完成项总数**：14 项（P0 × 2 / P1 × 6 / P2 × 4 / 待确认 × 2 已并入 P2）
- **其中阻塞主流程**：3 项（P0-001 装配漏登记、P0-002 重构未提交、P1-004 分布式化）
- **其中依赖外部服务联调**：2 项（extras-storage 真实 MinIO/OSS/S3、extras-message 真实通道）
- **其中依赖尚未提供的数据/凭证**：3 项（CacheSyncListener 选型、NESTED 事务传播、重构替代方案）

### 建议推进顺序

```
第 1 步  P0-002 data-mybatis 重构提交（唯一硬阻塞，其他 P1 都在它下游）
   │
第 2 步  P0-001 自动配置漏登记（3 行改动，收益极大——3 组能力立即在生产可用）
   │      ├─ 同时补一个"从 spring.factories 元加载"的测试防回归
   │
第 3 步（可并行两线）
   ├─ 线① 分布式化：P1-001 CacheSyncListener 选型 → P1-004 WebExtras 分布式存储
   │      └─ 依赖 Redis 底座选型决策
   │
   └─ 线② 事务与查询：P1-005 NESTED 传播（TDD）+ P1-006 data-jdbc join
         └─ 依赖 data-core QueryWrapper 重构方向澄清（P2-003）
   │
第 4 步  P1-002 plugin USO 契约澄清 + P1-003 FlexEncryptProcessor.Decrypt 策略补齐
   │
第 5 步  P2-001 message sender 下游实现样例 + P2-002 RuntimeHints 各模块拆分
   │
第 6 步  P2-004 文档统一修正（以修复后代码为准，一次性对齐）
```

关键依赖规则：
1. **P0-002 → 一切 data 相关 P1**：重构未提交前，data 层的其他修复会返工。
2. **P1-001 → P1-004**：分布式幂等/限流/验证码共用 Redis 底座，先做选型。
3. **P0-001 与 P1-001 是两条正交线**：前者是"没被装配"，后者是"被装配了但只有单机实现"——两个都需要做。

---

## 七、"待确认"项（需用户补充上下文）

| 项 | 需补充的上下文 |
|---|---|
| data-core QueryWrapper 重构目标 | 工作区已删 `query/QueryWrapper.java`，但无替代类——是彻底改走 LambdaQueryWrapper，还是保留一个更小的 API？请给出目标形态，否则 P1-006 join 实现无从下手 |
| data-mybatis 重构后是否还需要 FlexRepository/FlexJoinRepository | 旧审计说这两个类的行为有缺陷；如果重构方向是"下游直接用 Flex MyBatis 原生 API"，那旧审计 P0-C-①②③ 三项自动消失 |
| CacheSyncListener 分布式底座选型 | Redis pub/sub / Redisson topic / Redis Sentinel 事件——哪种？选型决定实现方案与测试策略 |
| 事务 NESTED 传播是否为必要能力 | 如果框架约定"只支持 REQUIRED/REQUIRES_NEW/NESTED（Spring 默认）"，需明确；否则 P1-005 是文档漂移而非功能缺失 |

---

## 八、方法与已知偏差

- 团队成员因 429 限流全部失败，**主理人直接基于工具链完成一手核实**；所有数字与结论均可用 `grep`/`git ls-tree`/`find` 复现。
- 未做编译与运行验证——本次仅静态只读分析，符合用户"不修改代码"的硬约束。
- 旧审计 09-01 的 P0-A/B/C/D 全部已修复或重构中，本次不重复报；剩余 P1/P2 已交叉核实。
- 未完成项的判定基于："空方法体"、"抛 UnsupportedOperationException"、"abstract 骨架"、"无实现类"、"接口定义了但缺生产实现"、"README 承诺但代码缺失"、"测试盲区导致生产不可用"——共 6 条独立判据。
- 显式 `TODO/FIXME/XXX/HACK` 在全仓 grep 结果仅 5 处，**均非占位实现**（如 PluginManager L45 是引导性设计），不构成未完成项。
- 全仓 `@Disabled` / `@Ignore` 注解 **0 处**——测试套件完全启用，无 flaky 治理风险（这一点修正了旧审计 P1-11 的"12 个测试文件依赖 Thread.sleep"担忧——已用 Awaitility 替代，工作区变更体现）。
