# Jowen Framework 未完成工作全面审计报告

> 审计日期：2026-09-01 · 审计方式：只读静态分析（架构师：代码级 611 个主源码文件 + 20 份文档；QA：JaCoCo 报告 545 类 +
> surefire 2482 条测试记录 + pom 配置）
> 审计团队：软件开发团队（主理人 齐活林 / 架构师 高见远 / QA 严过关）

---

## 一、总体结论（TL;DR）

1. **整体健康度中等偏上，但存在两类最危险的债务**：
    - **P0 级"静默错误结果"缺陷 3 处**（均在 data-mybatis）：联表查询失败时回退全表、WHERE 条件字符串拼接进 SQL、扩展点 fire
      链未接通导致加密/审计/逻辑删除/租户四项能力实质失效——这些都不会报错，生产事故将表现为"数据不对/防护失效"。
    - **覆盖率工具链断裂**（用户当前 P1 主线"覆盖率门禁"的直接阻塞项）：全仓库 pom 无 `jacoco prepare-agent` 配置，今日 2482
      个测试 0 失败但 **未产生任何 jacoco.exec**；framework-coverage 模块未列入根 pom reactor，默认构建永不生成聚合报告。
2. **文档漂移是系统性问题**：根 README、extras/boot-web README、docs/code-analysis.md 均与代码不一致。尤其
   docs/code-analysis.md 的关键论断已 **反向过时**（其记载 EventBus 不支持父类匹配，实际代码已支持；其"7 大风险"中 5
   个已修复但未更新）。
3. **覆盖率报告数据已滞后**：JaCoCo 聚合报告生成于 08-31 15:53，而 09-01 新增落盘测试约 130 个文件（i18n 25/28、data-jdbc
   47/56 等）晚于报告——报告中 i18n 48%、data-jdbc 51% **严重低估当前状态**，任何覆盖率阈值决策必须先重跑报告。
4. 代码中显式 TODO 标记仅 1 处；未完成工作主要以 **空方法体、静默降级回退、注册但永不触发的处理器**形式存在，比裸 TODO 更隐蔽。

---

## 二、主理人先行线索核实结果

| 线索                                    | 核实结果                                                                                             |
|-----------------------------------------|------------------------------------------------------------------------------------------------------|
| QueryWrapperTranslator join TODO        | ✅ 属实（`QueryWrapperTranslator.java:23`，join 被静默忽略）                                         |
| JdbcTransactionManager NESTED 简化      | ✅ 属实（L47 简化为 REQUIRED，无保存点）                                                             |
| FlexEncryptProcessor decrypt 缺省抛异常 | ✅ 属实，且牵出更大的扩展点 fire 链断裂问题（见 P0-C）                                               |
| EventBus 不回溯父类                     | ⚠️ **已过时（反向）**：`EventBus.java:129-135` 现已 `isAssignableFrom` 支持父类/接口匹配，是文档错了 |
| docs/code-analysis.md 一致性            | ⚠️ 大面积过时（详见 P2-文档项）                                                                      |

---

## 三、统一优先级清单（代码级 × 测试级交叉汇总）

### P0 —— 阻断当前主线 / 功能正确性

| 编号     | 模块          | 问题                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | 当前状态                             | 影响范围                                                                    | 工作量                                                          | 来源          |
|----------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|-----------------------------------------------------------------------------|-----------------------------------------------------------------|---------------|
| **P0-A** | 构建/coverage | 覆盖率工具链断裂：根 pom 无 jacoco prepare-agent、无 pluginManagement 版本锚定、framework-coverage 未入 reactor（其 pom 声明 parent=framework 且注释要求"最后构建"，但根 pom modules 未收录）；今日 2482 测试未产生任何 jacoco.exec                                                                                                                                                                                                                                                                         | 工具链不可复现，报告只能手工偶发产生 | **覆盖率门禁主线直接受阻**；当前全部覆盖率数字不可复现                      | S                                                               | QA+架构双确认 |
| **P0-B** | 构建/coverage | 覆盖率门禁规则缺失：无 jacoco:check、无阈值                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | 门禁实际不存在，只有报表没有闸门     | 同上                                                                        | S~M（建议 BUNDLE 行覆盖 ~70% 起步渐进收紧）                     | QA            |
| **P0-C** | data-mybatis  | ① `FlexJoinRepository.java:41-74`：selectJoinList 把框架 wrapper 直接反射传给 Flex selectList，异常时**回退 selectAll() 返回全表**；`selectList(wrapper)` 完全忽略 wrapper；② `ConditionMapper.java:71-98`：WHERE 值**直接字符串拼接进 SQL**（SQL 注入面，与根 README §10.2 "预编译强制"承诺冲突）；③ `ExtensionRegistry.java:91,97-122`：`firePreSave` 空方法体，加密/审计/逻辑删除/租户四个处理器已注册但**任何 fire 钩子都不调用**——四项企业能力实质未接通；且 insertOrUpdate 触发审计双写，读路径无解密 | 代码已写、链路未通                   | 联表查询返回错误数据集；注入风险；加密/审计/租户能力全部失效                | **L**（需 join→Flex 转换设计 + fire 链补齐；建议拆 3 个子任务） | 架构师        |
| **P0-D** | cache         | `MultilevelCache.java:60-67`：L1 命中 `NullValue.INSTANCE`（非 null）直接泄漏给调用方；`nullTtl` 空值 TTL 未生效；L66/L79 判空逻辑是死代码                                                                                                                                                                                                                                                                                                                                                                  | 代码已写、行为错误                   | 开启空值缓存时调用方拿到占位对象而非 null；get() 返回契约变更需联动回归测试 | M                                                               | 架构师        |

### P1 —— 重要功能缺失 / 关键测试盲区

| 编号  | 模块               | 问题                                                                                                                                                                                                    | 状态              | 工作量                                               |
|-------|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|------------------------------------------------------|
| P1-1  | data-jdbc          | join 未实现（TODO）+ translate 恒生成 `SELECT *`（无投影）；data-core README 声称的 JoinType 能力 jdbc 侧无实现                                                                                         | 未实现            | L                                                    |
| P1-2  | data-jdbc          | NESTED 降级 REQUIRED，内层回滚污染外层；**且无任何 NESTED 测试**（其余传播级别均已测）                                                                                                                  | 半实现+零测试     | M（建议 TDD：先写失败测试锚定保存点语义）            |
| P1-3  | data-jdbc          | `TenantInterceptor.java:35`：租户条件拼 SQL 末尾，遇 ORDER BY/GROUP BY/LIMIT 语法错误；无 WHERE 的 SELECT 完全不过滤（跨租户泄漏面）                                                                    | 行为缺陷          | M                                                    |
| P1-4  | data-core          | `QueryWrapper.having()` 伪实现（塞进 groupBy 列表产生非法 SQL）；非 ResolvableFunction 列解析产出垃圾列名不报错                                                                                         | 行为缺陷          | S~M                                                  |
| P1-5  | cache              | `CacheSyncListener` 跨节点同步仅是接口，全工程无实现，但根 README 声称已有；RedissonCache/RedissonCacheManager 0% 无测试                                                                                | 空接口+零测试     | M~L                                                  |
| P1-6  | boot-web           | 幂等/限流/验证码存储全为本地内存单机实现（`WebExtrasAutoConfiguration.java:85-101,152-154`），多实例失效；docs 声称走 CacheManager 与代码不符                                                           | 单机实现+文档错   | M（依赖 P1-5 的 Redis 底座选型）                     |
| P1-7  | plugin             | `PluginManager.loadPlugins` 抛 UnsupportedOperationException（接口承诺与实现割裂）；`registerExtensionPoint` 空方法体；ExtensionScanner/PluginDescriptorLoader/HotSwapManager 0%~低覆盖                 | 接口割裂+测试盲区 | M~L                                                  |
| P1-8  | data-jdbc          | 无测试类大集合：LobHandler(0%)、SqlBuilder(0%)、BatchParameterBinder(0%)、DefaultTypeHandlers 十余个 Handler(0%)、IdGenerators(0%)；QueryWrapperTranslator 覆盖率 35.8%，where/order/参数组合大面未覆盖 | 测试盲区          | M                                                    |
| P1-9  | data-mybatis       | 仓储层与代码生成器整体薄弱：FlexRepository/FlexJoinRepository/FlexDynamicRepository 均 0%，FlexTenantHandler 9%，codegen 三件套 ~5%（约 114 行）——**与 P0-C 的存量缺陷叠加：坏代码+零测试**             | 测试盲区          | L（与 P0-C 实现配套补）                              |
| P1-10 | data-jdbc          | `FlexDynamicRepository.java:32-52`：反射调用 SqlSessionFactory 上不存在的方法，且未接入任何 AutoConfiguration，属孤码                                                                                   | 孤码              | M（处置：修复或删除）                                |
| P1-11 | 跨模块             | 脆弱测试治理：12 个测试文件依赖 Thread.sleep（无 Awaitility）；LeakyBucketRateLimiterTest 墙钟断言最长 700ms；FileWatchResourceWatcherTest 文件系统时序敏感                                             | 测试脆弱          | M（**门禁上线 CI 前必须治理**，否则 flaky 阻塞主干） |
| P1-12 | 跨模块             | 静态/ThreadLocal 状态污染：FlexEncryptProcessorTest 改静态 setDefaultKey（含置 null 用例），多处测试直接操纵静态上下文，与未来并行执行冲突                                                              | 测试脆弱          | S~M                                                  |
| P1-13 | boot-autoconfigure | 分支覆盖仅 51%：PluginEndpoint 7.4%、PluginHealthIndicator 12%、S3/Oss/Minio 三个存储配置类 0%、LoggerBootstrap 21.4%                                                                                   | 装配分支盲区      | M                                                    |

### P2 —— 改进项 / 文档一致性

| 编号 | 问题                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 工作量                                      |
|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| P2-1 | `FlexEncryptProcessor`：AES/**ECB** 模式（安全弱）；加解密 catch 后返回原文（密钥错误时明文落库）——**必须在 P0-C 接通加密链路时一并处理**，否则正式启用不安全实现                                                                                                                                                                                                                                                                                                                                                                                                              | S（随 P0-C）                                |
| P2-2 | SQL 细节：NOT 分支生成非法 SQL（`QueryWrapperTranslator.java:57-63`）；`OFFSET n LIMIT m` 顺序仅 PG 合法（`FlexQueryWrapperTranslator.java:53-64`）                                                                                                                                                                                                                                                                                                                                                                                                                            | S                                           |
| P2-3 | `MultilevelCache.putIfAbsent` 只查 L1 跨节点不原子；PluginLifecycleManager 集合非线程安全；FlexRepositoryAdapter 反射缺判空 NPE                                                                                                                                                                                                                                                                                                                                                                                                                                                | S                                           |
| P2-4 | 方言与小类覆盖：OracleDialect 19%、SQLServerDialect 24%、EntityMetadata 0%、SmartLifecycle 22% 等                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | S                                           |
| P2-5 | framework-demo：19 文件/1388 行 0 测试——决策"排除出统计"或"补最小冒烟测试"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | S                                           |
| P2-6 | **系统性文档漂移**：① 根 README §8.9 称 extras 含 4 子模块，实际 3 个（web 在 framework-boot-web）；② boot-web README 标题仍是"framework-extras-web"且错误声称"基于 framework-cache 存储底座"；③ extras README 能力表缺 crypto/@Encrypt、sign/@Sign；extras-message README §4.2 列 4 个发送器实际 7 个；④ 根 README §8.3-8.7 多处类名/包结构与实际不符；⑤ message sender 均为抽象骨架但文档暗示即开即用；⑥ docs/code-analysis.md 行号引用、EventBus 行为、7 大风险状态全部过时（自称"8 模块 455 文件"，实际 611）；⑦ 根 README 模块清单缺 framework-demo 与 framework-coverage | M（**放最后**，以修复后代码为准一次性对齐） |
| P2-7 | data-core datapermission 包与 extras-web datapermission 职责重叠，归属需澄清（架构债）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | 需决策                                      |

---

## 四、模块健康度视图

| 模块                               | 未完成项密度            | 覆盖率（滞后数据）                              | 一句话诊断                              |
|------------------------------------|-------------------------|-------------------------------------------------|-----------------------------------------|
| framework-data-mybatis             | **最高（3×P0）**        | 62%（仓储层 0%）                                | 坏代码 + 零测试，最高风险区             |
| framework-data-jdbc                | 高（P1×4 + 测试盲区大） | 51%（今日已大幅补测，实际更高）                 | join/NESTED/租户三缺口 + 核心类覆盖不足 |
| framework-cache                    | 中高（P0-D + P1-5）     | 79%                                             | 多级缓存行为缺陷 + 同步空接口           |
| framework-boot(-web/autoconfigure) | 中（P1-6/P1-13）        | 95% / 68%                                       | 功能可用但单机化，装配分支盲区          |
| framework-plugin                   | 中（P1-7）              | 73%（9 个 0% 类）                               | 接口割裂 + 扫描/热插拔裸奔              |
| framework-core                     | 低                      | 87%                                             | 基本健康（EventBus 已修，仅文档过时）   |
| framework-i18n                     | 低（疑似）              | 报告 48% **严重滞后**（25/28 测试文件晚于报告） | 待重跑报告后重新评估                    |
| framework-logger / extras-*        | 低                      | 87~97%                                          | 基本健康                                |
| framework-coverage                 | **管道断裂（P0-A/B）**  | —                                               | 门禁主线的第一阻塞点                    |
| framework-demo                     | 无测试                  | 0%                                              | 待决策（P2-5）                          |

---

## 五、依赖关系与推荐执行顺序

```
第 1 步  P0-A 覆盖率工具链接通（prepare-agent + coverage 入 reactor + mvn verify 刷新报告）
   │      └─> P0-B 基于新报告定阈值上线门禁（先修管道再定基线，避免按 i18n 48% 旧数字误判）
   │
第 2 步（可并行三线）
   ├─ 线① data 查询层：P0-C-② ConditionMapper 参数化（最先，其他条件翻译都建在其上）
   │        └─> P0-C-① FlexJoinRepository join 翻译 + P1-1 jdbc join + P1-4 having 修复
   ├─ 线② data-mybatis 扩展点：P0-C-③ fire 链补齐（+P2-1 ECB 一并处理）
   │        └─> P1-9 仓储层/codegen 测试配套补齐
   └─ 线③ cache：P0-D NullValue 修复 ──> P1-5 跨节点同步选型 ──> P1-6 分布式幂等/限流
   │
第 3 步  P1-11/P1-12 脆弱测试治理（门禁在 CI 常态化运行前完成）
   └─> P1-2 NESTED、P1-3 租户、P1-7 plugin 接口、P1-8 测试盲区、P1-10 孤码处置
   │
第 4 步  P2-6 文档统一修正（以修复后代码为准，一次性对齐，避免二次漂移）
```

关键依赖规则：

1. **P0-A → P0-B**：阈值必须基于刷新后的报告，否则误判。
2. **P0-C-②（参数化）→ 一切条件翻译类工作**：先堵注入面再叠功能，避免返工。
3. **P1-5 → P1-6**：分布式幂等/限流/验证码共用 Redis pub/sub 底座，先做选型。
4. **P2-1 必须随 P0-C-③ 同步做**：把 ECB 加密正式接通等于固化不安全实现。
5. **P2-6 放最后**：文档以终态代码为准。

---

## 六、风险提示

1. **静默降级是最大隐患**（架构师+QA 共识）：FlexJoinRepository
   回退全表、加密失败返回原文、限流/幂等本地降级都不抛错——生产事故表现为"数据不对/防护失效"，排障成本极高。
   **短期建议：先把这些降级路径改为可配置 fail-fast 或至少 WARN 日志（S 工作量，可立即做）。**
2. **覆盖率数字失真**：当前报告滞后一天，基于旧报告排优先级会重复劳动（i18n 可能已大幅改善）。
3. **门禁上线与 flaky 冲突**：12 个 sleep 依赖测试在 CI 慢机上会间歇性失败，门禁常态化后直接阻塞主干——P1-11 必须前置。
4. **兼容性风险**：P0-C 接通加密链路涉及存量 ECB 密文迁移；P0-D 修复改变 get () 返回契约——均需 QA 联动回归（QA 已列入建议）。
5. **文档信任危机**：docs/code-analysis.md
   被当权威参照但关键论断已反向（EventBus），继续引用会误导决策——建议立即加"已过时"横幅（S），重写放第 4 步。
6. **技术债线性恶化**：data 查询层（注入面 + join 缺失）若业务先在其上叠加功能，返工成本随使用量线性增长。

---

## 七、下一步建议（Top 5）

1. **立即执行 P0-A**（S，半天内）：根 pom 补 jacoco prepare-agent + pluginManagement 版本锚定 + framework-coverage 入
   reactor，跑 `mvn verify` 刷新报告——这是覆盖率门禁主线的第一块多米诺。
2. **为 3 处静默降级加 WARN 日志/fail-fast 开关**（S，与 1 并行）：低成本消除最大生产隐患。
3. **启动 data-mybatis 修复线**（L，建议拆 3 个子任务单独排期）：ConditionMapper 参数化 → fire 链接通（含 ECB 治理）→ join
   翻译，全程 TDD（QA 已给出先写失败测试锚定期望的具体建议）。
4. **门禁上线前完成 flaky 治理**：引入 Awaitility + 可注入时钟，替换 12 处 sleep 断言。
5. **docs/code-analysis.md 先贴"已过时"横幅**，全面文档对齐放收尾。

---

*附：本报告由架构师（代码级）与 QA（测试级）两份审计交叉汇总而成，全部结论附有文件路径/行号/覆盖率数据出处，无估算编造。*
