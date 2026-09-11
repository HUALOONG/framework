# Jowen Framework 未完成项现状盘点（2026-09-11）

> 基线：HEAD `209c5c2`（`fix: 补提交 framework-cache/support 下重名 ConditionEvaluator 删除`）
> 验证：`mvn -o clean verify`（`/tmp/full-verify3.log`，BUILD SUCCESS，`Rule violated` = 0）

---

## 一、结论先行

**剩余未完成项：4 项**（09-05 审计报告列出 5 项，其中 P2-004b 已完结）。
无 P0 级阻塞项；**唯一 P1 级功能性缺口是限流集群化（P1-004b）**。

本轮（09-05 → 09-11）新增且已完成：
- cache 模块 `condition` / `unless` SpEL 条件评估（原评估结论"未完成"，已归零）
- P1-004c 真实 Redis 集成测试的架构设计文档（代码层面 `RedissonCommandExecutorIntegrationTest` 基类早前已落地）

---

## 二、逐项核实（逐条复核代码，非查旧文档）

### 🔴 P1-004b — 限流集群化（仍待办）

**现状**：`framework-boot-web/ratelimit/` 下 12 个类，进程内计数为主。
- `FixedWindowRateLimiter`、`SlidingWindowRateLimiter`、`LeakyBucketRateLimiter`、`TokenBucketRateLimiter`：纯进程内实现。
- `RedisFixedWindowRateLimiter`、`RedisTokenBucketRateLimiter`：**已存在** Redis 实现，但仅覆盖 2/4 算法。
- `RateLimitScripts.java` 已预置 Lua 脚本，说明架构上预留了脚本化路径。

**缺口**：滑动窗口（需 Redis ZSET）与漏桶（需 Lua token bucket）的 Redis 化；以及 `RedisCommandExecutor` 接口是否需扩展 `zadd`/`zremrangeByScore`/`eval`。

**关键约束**（来自 09-05 决策，待你确认）：
- 一致性语义：推荐**最终一致**（限流毫秒级误差无实质影响，强一致会引入 Redis 往返成为热点）
- `RedisCommandExecutor` 扩展：推荐以 `default` 方法提供 `eval(...)`（抛 `UnsupportedOperationException`），非 breaking

### 🟡 P2-001 — message 7 个 sender 补实现（仍待办）

**现状核实**（`framework-extras-message/provider/`）：

| 类 | 状态 |
|---|---|
| `DingTalkMessageSender` | `abstract` 骨架 |
| `EmailMessageSender` | `abstract` 骨架 |
| `PushMessageSender` | `abstract` 骨架 |
| `SiteMessageSender` | `abstract` 骨架 |
| `SmsMessageSender` | `abstract` 骨架 |
| `WeComMessageSender` | `abstract` 骨架 |
| `WebhookMessageSender` | `abstract` 骨架 |
| **`HttpWebhookMessageSender`** | ✅ **已落地**（`extends WebhookMessageSender`，JDK 内置 `HttpClient`，JSON 信封/裸文本双模式） |

故"7 个骨架"的说法需修正：**6 个抽象骨架 + 1 个已落地实现**。
落地路径：Webhook 零外部依赖，可立即做；其余 6 个需各渠道 API 凭证与协议细节。

### 🟢 N-003 — `Notification` 配置块空转（仍待办）

**现状核实**：`ExtrasWebProperties.Notification` 仅作为 POJO 字段存在（L483 Javadoc 已自认"框架通知能力未落地"），全 `framework-boot-web` 无任何实现类或装配逻辑引用它。配置写入静默不生效。

**建议处置**：删除该配置块（YAGNI），避免误导使用方——与 09-05 主理人推荐一致。

### 🟢 P2-004b — `docs/code-analysis.md` 过时（✅ 已完结）

**现状核实**：文件头部已加醒目标注——"历史快照（截至 2026-09-05）……已过时、仅供参考……请勿基于本文逐行更新现状文档"。534 行，状态已从"待办"转为"已标记"，无需再改。

---

## 三、本轮归零项

### cache 的 `condition` / `unless` 条件评估

09-05 审计报告未将其列为独立项（当时评估为"骨架"），09-10 评估确认完成度约 10%。现已端到端落地，提交 `9dad88e`：

- `framework-cache/condition/ConditionEvaluator.java`：抽象类，零 Spring 依赖；`evaluate`（空/空白→恒成立）/ `evaluateUnless`（空/空白→恒不成立）默认方法 + 抽象 `doEvaluate(String, CacheOperationContext)`
- `framework-boot-autoconfigure/SpelConditionEvaluator.java`：SpEL 实现，注入 `#target` / `#a0` / `#p0` / 参数名变量；`toBoolean` 处理 Boolean/Number/String 转换
- `SpringCacheAnnotationProcessor`：三处 `@Around` 接入 `passesCondition`（condition 为假 → 只执行方法体、跳过缓存；unless 为真 → 跳过缓存；无 Bean → 回退无条件通过）；`@CacheEvict` 的 evict 加 `if (passesCondition(ctx))` 守卫
- 根 `pom.xml`：compiler 插件加 `<parameters>${maven.compiler.parameters}</parameters>`（此前属性声明了但未被引用，导致 `#id` 类参数名变量求值为 null）
- 测试：`ConditionEvaluatorTest`（抽象层契约）+ `SpelConditionEvaluatorTest`（11 用例）+ `SpringCacheAnnotationProcessorTest` 追加 10+ 条件用例

**踩坑**：`SpelConditionEvaluator.buildContext` 最初只注册 `#a0/#p0`，漏了参数名变量，导致 `#id == 'u-1'` 失败；用 `SpelProbeTest3` 隔离验证后补上。

### P1-004c — 真实 Redis 集成测试（设计文档已提交）

`docs/system_design.md`（347 行）+ `class-diagram.mermaid` / `sequence-diagram.mermaid` 已随 `9dad88e` 提交。
代码侧 `RedissonCommandExecutorIntegrationTest` 基类早前已落地（`@Tag("integration")`，需 `-Dtest.redis.url`，默认跳过，不阻塞门禁）。

**差距**：设计文档规划的 5 个测试类（`RedisFixedWindowRateLimiterIntegrationTest`、`RedisTokenBucketRateLimiterIntegrationTest`、`RedisIdempotentStoreIntegrationTest`、`RedisCaptchaStoreIntegrationTest`）尚未编写；当前仅基类 + 4 个 `Nested` 子套件（`SetIfAbsentAndGet` / `SupportsScript` / `DeleteIfMatch` / `Eval` / `Ttl` / `DistributedLockSemantics` / `Delete`，全部 skip）。

---

## 四、覆盖率门禁（独立口径，`mvn -o clean verify`）

| 模块 | 行覆盖率 | 备注 |
|---|---|---|
| framework-data/data-core | 100.00% | |
| framework-data/data-mybatis | 99.64% | |
| framework-extras/extras-storage | 98.43% | |
| framework-data/data-jdbc | 98.38% | |
| framework-logger | 97.96% | |
| framework-boot/boot-web | 97.86% | |
| framework-core | 96.84% | |
| framework-plugin | 96.30% | |
| framework-extras/extras-common | 96.22% | |
| framework-cache | 96.13% | 含本次新增的 ConditionEvaluator |
| framework-boot/boot-autoconfigure | 95.96% | 去重重构后回落 10 行，仍过门槛 |
| framework-extras/extras-message | 95.90% | |
| framework-i18n | 95.49% | **余量最紧（0.49pt）** |

13/13 全部 > 95%，`Rule violated` = 0。**最脆弱模块是 i18n（95.49%）**；后续在 `boot-autoconfigure` 新增 Bean 装配分支时必须同步补测试。

---

## 五、推进建议（按风险/价值排序）

```
第 1 步  🔴 P1-004b 限流集群化 — 先做架构决策（你确认一致性语义 + 接口扩展方式），再实现
         └─ 依赖：RedisCommandExecutor 可能需扩展 ZSET/Lua 能力
第 2 步  🟡 P2-001 message sender — 先做 HttpWebhook 之外的渠道（Webhook 已落地）
         └─ 与 P1-004b 独立，可并行
第 3 步  🟢 N-003 删除空转的 Notification 配置块 — 纯删除，零风险
         └─ 建议与 P1-004b 同 PR（都动 WebExtrasAutoConfiguration 区域）
第 4 步  🟢 P1-004c 真实 Redis 集成测试 — 补 4 个测试类
         └─ 需 Upstash 凭证；与 P2-001 独立
```

**无需再做**：P2-004b（`docs/code-analysis.md` 已标记为历史快照）。