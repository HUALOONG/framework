# 变更总结（Jowen Framework · 2026-09-04）

> 对应问题清单：`docs/unfinished-work-inventory-2026-09-04.md`（14 项）
> 逐条分析：`docs/fix-plan-2026-09-04.md`
> 本文件为最终变更总结，覆盖已落地修复 + 仍需决策/收尾的项。

## 1. 已提交修复（按优先级）

| 项 | 优先级 | 修复内容 | 提交 |
|----|--------|----------|------|
| P0-001 | 🔴 | `AutoConfiguration.imports` 补登 3 个 `@AutoConfiguration`（FileStorage/Message/WebExtras） | `fb3ae56` |
| P0-002 | 🔴 | data 层跨模块重构提交（core→jdbc→mybatis 分 3 提交） | `20b721f` `7bd6621` `dcb384c` |
| P1-001 | 🟡 | 跨节点缓存同步：Redis pub/sub（Redisson RTopic）实现 + 装配 | `291f510` (+ `532bfcb` 编译修复) |
| P1-002 | ⚪ | 插件 lifecycle 引导性 USO 仅补文档（by-design，不改实现） | `6ce8764` |
| P1-003 | ⚪ | `FlexEncryptProcessor` 扩展点默认抛出仅补文档（by-design） | 随 `dcb384c` |
| P1-005 | 🟡 | NESTED 事务传播（基于 JDBC 保存点） | `1a779db` |
| P2-002 | 🟢 | RuntimeHints 覆盖 core/plugin SPI 与 extras 7 个发送器 | `9330cab` |
| P2-004 | 🟢 | extras-message README 发送器数量修正（4→7） | `2cd3aa6` |

## 2. 关键实现说明

### P0-002（data 重构）— 主流程阻塞解除
- **范围修正**：文档原述"data-mybatis 11 文件"，实际为 `framework-data-core / data-jdbc / data-mybatis` 三模块共 93 处删除+重写（query/repository/transaction 整包删除、Flex* 系列删除、ExtensionRegistry 重写、新增 `Encrypted` 注解）。
- **验证**：提交前已跑通三模块 `mvn test`（134 测试全绿，含 MyBatis Flex 相关 21+17 项）。
- **提交粒度**：按用户决策 core→jdbc→mybatis 分 3 提交，便于 review/回滚。

### P1-001（跨节点缓存同步）
- 新增 `CacheSyncMessage` / `CacheSyncBroadcaster` / `CacheSyncBroadcasterFactory` / `RedissonCacheSyncListener` / `SyncPublishingCache` 装饰器。
- `MultilevelCacheManager` 支持 `setCacheSyncBroadcasterFactory`；`CacheAutoConfiguration` 在 `framework.cache.multilevel.sync.enabled=true` 且 classpath 有 Redisson 时自动装配（默认关闭，向后兼容）。
- 复用模块既有 Redisson 依赖（即 Redis pub/sub，契合选型）。3 个 Mockito 单测（无需真实 Redis）。

### P1-005（NESTED 事务传播）
- 新增 `NestedTransaction`（AutoCloseable，基于 `Connection.setSavepoint`/`rollback(savepoint)`/`releaseSavepoint`）。提供 `commit()`（释放保存点）、`rollback()`（仅回退到保存点）、静态 `execute()`。
- 语义对齐 Spring `Propagation.NESTED`，契合重构后的轻量事务模型（不重新引入 `Propagation` 枚举/完整事务管理器）。
- 7 个 Mockito 单测全绿。

### P2-002（RuntimeHints）— 架构修正
- **重要修正**：原计划"在 core/extras/plugin 各模块补 registrar"不可行——这些模块**刻意保持 Spring 无关**（无 spring-core 依赖），无法承载 `RuntimeHintsRegistrar`。
- **实际落点**：沿用既有 6 个 registrar 集中登记于 `boot-autoconfigure` 的模式，扩展 `SpiRuntimeHints`（新增 core.spi 扩展源、plugin.spi 桥接类）并新增 `ExtrasRuntimeHints`（7 个消息发送器），在 `aot.factories` 登记。仅影响 GraalVM 原生镜像，不影响 JVM 运行。

## 3. 验证证据

| 模块/场景 | 命令 | 结果 |
|-----------|------|------|
| data-core/jdbc/mybatis | `mvn -o -pl ... -am test` | BUILD SUCCESS，134 测试 |
| framework-cache（P1-001） | `mvn -o -pl framework-cache -am test` | BUILD SUCCESS，152 测试 |
| framework-data-jdbc（P1-005） | `mvn -o -pl framework-data-jdbc -am test` | BUILD SUCCESS，410 测试（含 7 NestedTransactionTest） |
| framework-boot-autoconfigure（P1-001 装配修复 + P2-002） | `mvn -o -pl ... -am test-compile` | BUILD SUCCESS |

## 4. 仍需决策/收尾的项

| 项 | 状态 | 建议 |
|----|------|------|
| P1-004 WebExtras 分布式 | 🟡 待范围 | 幂等/限流/验证码/密钥存储当前为单实例；需确认将哪些迁至 Redis 共享存储（复用 P1-001 底座）。**属较大改动，建议单独排期**。 |
| P1-006 SQL 联表 | ✅ 已由 P0-002 消解 | QueryWrapper 已删除，联表能力转由 MyBatis Flex 原生 API 提供；无需自研 join 层。建议在 README 注明。 |
| P2-001 消息样例 | ⚪ by-design | 7 个 sender 为模板方法骨架（设计意图）；可选补 1~2 个样例（Webhook/Site）或仅收敛 README 表述。 |
| P2-003 QueryWrapper 形态 | ✅ 已由 P0-002 消解 | 目标形态 = 彻底转向 MyBatis Flex 原生 API。 |
| P2-004 其余文档 | 🟡 部分完成 | extras-message 已修；`boot-web/README`（标题误为 framework-extras-web + 误称基于 framework-cache 底座）、`docs/code-analysis.md`（行号/EventBus/风险状态过时）仍待修——**此二文件已被既有未提交 WIP 占用，未擅自修改以免夹杂，建议并入该 WIP 一并处理**。 |
| P0-001 回归元测试 | 🟢 可选 | 建议新增"从 imports 资源全量加载装配类"元测试，防再次漏登。 |

## 5. 工作树说明（重要）

本任务**仅提交了我亲自改写的文件**。工作树当前仍有约 80 处未提交改动，属**既有 WIP**（含 boot-web 脱敏/Excel/Plugin 端点、各模块预存测试、README/BOM/coverage pom 调整等），**非本任务范围、非我所写**，已刻意不触碰，以免夹杂或引入未知冲突。建议该 WIP 由对应负责人单独 review/提交。

## 6. 提交清单（本任务）

```
2cd3aa6 docs(extras-message): P2-004 修正渠道发送器数量为 7（原误写 4）
9330cab feat(boot): P2-002 扩展 RuntimeHints 覆盖 core/plugin SPI 与 extras 消息发送器（集中登记）
532bfcb fix(boot): P1-001 修正 CacheAutoConfiguration 中 BeanPostProcessor 非函数接口编译错误
6ce8764 docs(plugin): P1-002 澄清 lifecycle 引导性异常为 by-design（不改实现）
1a779db feat(data-jdbc): P1-005 NESTED 事务传播（基于 JDBC 保存点）
fb3ae56 fix(boot): P0-001 补登 3 个 @AutoConfiguration + P1-001 缓存同步自动装配
291f510 feat(cache): P1-001 跨节点缓存同步（Redis pub/sub via Redisson RTopic）
dcb384c refactor(data-mybatis): 移除 Flex* 包装/适配器层，重写 ExtensionRegistry，新增 Encrypted 注解
7bd6621 refactor(data-jdbc): 移除 JdbcRepository 体系，重构事务同步管理器并调整 ID 生成
20b721f refactor(data-core): 移除 query/repository/transaction 抽象层，统一收敛至 MyBatis Flex 原生 API
```
